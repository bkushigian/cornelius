use egg::*;
use serde::Deserialize;
use crate::rewrites::RewriteSystem;
use serde_aux::prelude::*;
use serde_xml_rs::from_reader;
use crate::peg::{Peg, PegAnalysis};
use std::collections::{HashMap, HashSet};

use log::Level;

/// `Subjects` represents a collection of methods and associated mutants for
/// Cornelius to run on as well as associated lookup information.
#[derive(Debug, Deserialize)]
pub struct Subjects {

  /// The subjects that Cornelius will run on.
  #[serde(rename = "subject", default)]
  pub subjects: Vec<Subject>,

  /// A `Vec` of IdTableEntries that represent the raw parsed id table
  pub id_table: IdTable,

  /// A table of node equivalences detected in the front end.  These usually
  /// come from Theta nodes, which are cyclic but need to be de-cyclified before
  /// being loaded into the egraph
  #[serde(rename = "NodeEquivalences", default)]
  pub node_equivalences: Vec<NodeEquivalence>,
}

impl Subjects {

  /// Compute a `RecExpr` from `self.id_table`, storing it in
  /// `self.rec_expr`.
  pub fn compute_rec_expr(&self) -> Result<RecExpr<Peg>, String> {
    info!("Computing RecExpr");
    let mut rec_expr = RecExpr::default();
    //println!("Computing rec_expr from Subjects");
    for entry in self.id_table.entries.iter() {
      let peg: Peg = parse_peg_from_string(entry.peg.clone())?;
      //println!("Adding entry {} as peg {:?}", entry.peg, peg);
      rec_expr.add(peg);
    }
    info!("Returning RecExpr");
    Ok(rec_expr)
  }

  pub fn num_mutants(&self) -> usize {
    let mut n = 0;
    for s in &self.subjects {
      n += s.mutants.len()
    }
    n
  }
}

#[derive(Debug, Deserialize)]
pub struct NodeEquivalence {
  pub first: String,
  pub second: String,
}

#[derive(Debug, Deserialize)]
pub struct IdTable {
  #[serde(rename = "dedup_entry")]
  pub entries: Vec<IdTableEntry>
}

/// An entry in a map from Peg IDs to the dereferenced Peg form. These are
/// parsed directly as raw strings.
#[derive(Debug, Deserialize)]
pub struct IdTableEntry {
  pub id: String,
  pub peg: String,
}

/// Given a string representing a peg, try to parse it and return a Result
fn parse_peg_from_string(peg_str: String) -> Result<Peg, String> {
  let peg_str =
  if peg_str.starts_with('(') {
    if !peg_str.ends_with(')') {
      return Err(format!("Mismatched PEG string {}", peg_str));
    }
    &peg_str[1..(peg_str.len()-1)]
  } else if peg_str.starts_with('"') {
    if !peg_str.ends_with('"') && peg_str.len() > 1 {
      return Err(format!("invalid String literal {}", peg_str));
    }
    return Ok(Peg::Symbol(Symbol::from(&peg_str[1..(peg_str.len()-1)])));
  } else {
    &peg_str
  };

  let split_peg_str: Vec<&str> = peg_str.split(' ').collect();
  let op = split_peg_str.get(0).expect("Empty peg application");
  let children: Vec<_> = split_peg_str[1..]
    .iter()
    .map(|s|
        Id::from(s.parse::<u32>()
                  .unwrap_or_else(|_| panic!("Couldn't parse u32 {} from {}", s, peg_str))
                  as usize))
    .collect();
  Peg::from_op_str(op, children)
}


#[derive(Debug, Deserialize)]
/// A subject for Cornelius to analyze. A subject includes the original method,
/// the set of mutants to test for equivalence and redundance, and metadata
/// tracking the source file and the name and signature of the method
pub struct Subject {
  #[serde(rename = "sourcefile")]
  /// Metadata about the source file that this is from
  pub source_file: String,

  /// The name and signature of the method
  pub method: String,

  /// The source code in an egg-readable format
  pub pid: String,

  #[serde(rename = "mutant")]
  /// Each of the mutants associated with this subject
  pub mutants: Vec<Mutant>,

  #[serde(default)]
  /// The results of analyizing this subject
  pub analysis_result: AnalysisResult,
}

impl Subject {
  /// Create a new `Subject`
  pub fn make(source_file: String, method: String, inputs: &[(u32, &str)]) -> Subject {
    let mut mutants = Vec::new();

    for (id, src) in inputs[1..].iter() {
      let m = Mutant {
        mid: *id,
        pid: (*src).parse().unwrap(),
      };
      mutants.push(m);
    }

    Subject {
      source_file,
      method,
      pid: String::from(inputs[0].1).parse().unwrap(),
      mutants,
      analysis_result: AnalysisResult::default(),
    }
  }

  pub fn from_file(path: String) -> Result<Subjects, String> {
    use std::fs;
    info!("Reading subject file from path {}", path);
    let contents = fs::read_to_string(path).unwrap();
    info!("Got subject file contents");
    //println!("contents: {}", contents);
    let subjects: Subjects = from_reader(contents.as_bytes()).unwrap();
    info!("Parsed subjects");
    Ok(subjects)
  }

}

/// The results of running an analysis. This is deriving Deserialize to make the
/// compiler happy, but an AnalysisResult is never actually deserialized.
#[derive(Debug, Default, Deserialize)]
pub struct AnalysisResult {
  /// Number of equivalences discovered
  pub score: u32,
  /// The equivalence classes discovered
  pub equiv_classes: Vec<HashSet<u32>>,
}

/// Represent a mutant of a subject's original code
#[derive(Debug, Deserialize)]
pub struct Mutant {
  /// The id of the mutant, as generated by Major
  #[serde(deserialize_with = "deserialize_number_from_string")]
  pub mid: u32,
  /// The PEG expression of the mutant
  pub pid: String,
}

/// Read in serialized info as a `Subjects` instance and run equality saturation
/// on each `Subject` in the deserialized `Subjects`
pub fn run_on_subjects_file(subj_file: &str) -> Result<Subjects, String> {
    let subj_file = subj_file.trim();
    let rules = crate::rewrites::rw_rules();
    info!("Running on subject file {}", subj_file);

    let subjects: Subjects = Subject::from_file(subj_file.to_string())
        .expect("Error reading subjects");
    info!("Read in subjects");

    run_on_subjects(subjects, &rules)
}

pub fn run_on_subjects(mut subjects: Subjects, rules: &RewriteSystem) -> Result<Subjects, String> {
    // We compute a RecExpr<Peg> from the lookup table mapping ids to Peg
    // expressions. This RecExpr contains the original program and every mutant,
    // as well as every sub-expression used
    let rec_expr = subjects.compute_rec_expr()?;

    if log_enabled!(Level::Debug) {
        let rec_expr_ref = rec_expr.as_ref();
        for i in 0..rec_expr_ref.len(){
            let v = rec_expr_ref[0..(i+1)].to_vec();
            let re = RecExpr::from(v);
            debug!("re {}:   {}", i, re.pretty(40));
        }
    }

    /*  -*-*- Insert nodes into a new egraph -*-*- */

    let mut egraph = EGraph::<Peg, PegAnalysis>::default();

    // We need to keep track of updated ids. This can happen because nodes are
    // merged during an egraph.add()
    //
    // id_offset_map maps the original id (as expressed in the serialized subject)
    // to id returned by the egraph.
    let mut id_offset_map = HashMap::<Id, Id>::default();

    // Add each node to the egraph, keeping track of ids as we go
    for (idx, node) in rec_expr.as_ref().iter().enumerate() {
      // First, we mutably clone the node so that we can update its children
      // with the updated ids from the egraph.
      let mut node = node.clone();
      // Next, for each
      node.for_each_mut(|id: &mut Id| *id = {
        let child = id_offset_map.get(id);
        assert!(child.is_some(), "Node at idx {} has unbound child {}", idx, id);
        child.unwrap().clone()
      });

      let id = egraph.add(node.clone());
      let id = egraph.find(id);
      id_offset_map.insert(Id::from(idx), id);
    }
    debug!("egraph total_size after deserializing: {}", egraph.total_number_of_nodes());

    let runner = Runner::default().with_egraph(egraph);
    // let egraph_size = egraph.total_size();
    // if rec_expr_size != egraph_size {
    //     println!("rec_expr total_size: {}", rec_expr_size);
    //     println!("egraph total_size after deserialization: {}", egraph_size);
    //     println!("Deserialization error: Parsed RecExpr has size {} while the e-graph has size {}", rec_expr_size, egraph_size);
    //     if log_enabled!(Level::Debug) {
    //         let rec_expr_ref = rec_expr.as_ref();
    //         debug!("\nRexExpr:");
    //         debug!("--------");
    //         for i in 0..rec_expr_ref.len(){
    //             let v = rec_expr_ref[0..(i+1)].to_vec();
    //             let re = RecExpr::from(v);
    //             debug!("re {}:   {}", i, re.pretty(40));
    //         }
    //         debug!("\nEClasses:");
    //         debug!("---------");
    //         for c in egraph.classes() {
    //             debug!("{:?}", c);
    //             for n in &c.nodes {
    //                 debug!("    {:?}", n);
    //             }

    //         }
    //     }
    //     panic!("rec-expr and e-graph are different sizes")
    // }

    let runner = runner.run(rules);
    let egraph = &runner.egraph;
    debug!("egraph total_size after run: {}", egraph.total_number_of_nodes());

    let mut i: u32 = 1;
    for mut subj in &mut subjects.subjects {
        info!("");
        info!("---------------------------------------");
        info!("Running on subject {}:{}", subj.source_file, subj.method);
        info!("Analyzing results of subject {}", i);
        info!("    subject peg = {}", subj.pid);
        info!("---------------------------------------");
        info!("");
        if log_enabled!(Level::Info) {
          let rec_expr_ref = rec_expr.as_ref();
          let id: usize = subj.pid.parse().unwrap();
          let v = rec_expr_ref[0..(id + 1 as usize)].to_vec();
          let re = RecExpr::from(v);
          info!("original id: {}:\n{}", id, re.pretty(80));
          for m in &subj.mutants {
            let mid = m.mid;
            let pid: usize = m.pid.parse().unwrap();
            let v = rec_expr_ref[0..(pid + 1)].to_vec();
            let re = RecExpr::from(v);
            info!("mutant id: {}:\n{}", mid, re.pretty(80));
          }
        }
        analyze_subject(&mut subj, egraph, &rec_expr, &id_offset_map);
        i += 1;
    }
    Ok(subjects)
}

/// Run on a subject and return number of identified equivalences.
/// This is defined to be the sum over each equivalence class C
/// the size |C| - 1:
///     sum_{C in Equiv Classes} |C| - 1
/// # Args
/// * `subj`: the subject to analyze
/// * `rules`: the `RewriteSystem` to run while analyzing
///
/// TODO We shouldn't be writing directly to a file---we should be returning a
/// structure that summarizes the analysis.
fn analyze_subject(subj: &mut Subject,
                   egraph: &EGraph<Peg, PegAnalysis>,
                   _expr: &RecExpr<Peg>,
                   id_update: &HashMap<Id, Id>
) {

    // Map canonical_ids (from egg) to mutant ids (from Major)
    let mut rev_can_id_lookup = HashMap::<Id, HashSet<u32>>::default();
    let mut num_equivalences = 0;

    // the PEG id
    let id: u32 = subj.pid.parse().unwrap();

    // Get the canonical id in the egraph for the subject, and compute the
    // set of equivalent ids (i.e., ids found to be equivalent to the subject)
    let id = Id::from(id as usize);
    let id = id_update.get(&id).unwrap().clone();
    let canonical_id = egraph.find(id);
    let equiv_ids = rev_can_id_lookup
        .entry(canonical_id)
        .or_insert_with(HashSet::default);
    equiv_ids.insert(0);

    for m in &subj.mutants {
        // PEG id
        let id: usize = m.pid.parse().unwrap();
        let id: Id = Id::from(id);
        let id = id_update.get(&id).unwrap();
        let canonical_id = egraph.find(*id);

        let equiv_ids = rev_can_id_lookup
            .entry(canonical_id)
            .or_insert_with(HashSet::default);
        equiv_ids.insert(m.mid);
    }

    let mut equiv_classes: Vec<HashSet<u32>> = vec![];
    for can_id in rev_can_id_lookup.keys() {
        let equiv_ids = rev_can_id_lookup.get(can_id).unwrap();
        let n = equiv_ids.len() as u32;
        num_equivalences += n - 1;
        equiv_classes.push(equiv_ids.clone());
    }

    subj.analysis_result = AnalysisResult {
      score: num_equivalences,
      equiv_classes,
    };
}
