pub mod subjects;
pub mod delta;
pub mod peg;
pub mod rewrites;
mod tests;

use egg::*;
use crate::subjects::{Subject, Subjects};
use crate::rewrites::RewriteSystem;
use crate::peg::{Peg, VarAnalysis};
use std::collections::{HashMap, HashSet};
use std::env;
use std::fs::File;
use std::io::prelude::*;
use itertools::Itertools;

#[macro_use] extern crate log;
use log::Level;

fn main() -> Result<(), String> {
    let _ = env_logger::builder().try_init();
    let rules = crate::rewrites::rw_rules();
    let args: Vec<String> = env::args().collect();
    let subject_files = &args[1..];

    for subj_file in subject_files {
        let subj_file = subj_file.trim();
        println!("Running on subject file {}", subj_file);

        let subjects: Subjects = Subject::from_file(subj_file.to_string())
            .expect("Error reading subjects");

        run_on_subjects(&subjects, &rules)?;
    }
    Ok(())
}

fn run_on_subjects(subjects: &Subjects, rules: &RewriteSystem) -> Result<(), String> {
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

    println!("rec_expr total_size: {}", rec_expr.as_ref().len());
    let runner = Runner::default()
        .with_expr(&rec_expr)
        .run(rules);
    let egraph = &runner.egraph;
    println!("egraph total_size: {}", egraph.total_size());

    let mut equiv_file = File::create("equiv-classes").map_err(|_| "Could not create file")?;
    for (i, subj) in subjects.subjects
        .iter()
        .enumerate()
    {
        println!("---------------------------------------");
        println!("Analyzing results of subject {}", i + 1);
        println!("    subject code = {}", subj.code);
        println!("---------------------------------------");
        analyze_subject(subj, egraph, &rec_expr, &mut equiv_file);
    }
    Ok(())
}
/// Run on a subject and return number of identified equivalences.
/// This is defined to be the sum over each equivalence class C
/// the size |C| - 1:
///     sum_{C in Equiv Classes} |C| - 1
/// # Args
/// * `subj`: the subject to analyze
/// * `rules`: the `RewriteSystem` to run while analyzing
/// * `equiv_file`: a `&File` that we write the equivalence classes to
///
/// TODO We shouldn't be writing directly to a file---we should be returning a
/// structure that summarizes the analysis.
fn analyze_subject(subj: &Subject,
                   egraph: &EGraph<Peg, VarAnalysis>,
                   _expr: &RecExpr<Peg>,
                   equiv_file: &mut File
) -> u32 {
    println!("Running on subject {}:{}", subj.source_file, subj.method);

    // Map canonical_ids (from egg) to mutant ids (from Major)
    let mut rev_can_id_lookup = HashMap::<Id, HashSet<Id>>::default();
    let mut num_equivalences = 0;

    // the PEG id
    let id: u32 = subj.code.parse().unwrap();

    // Get the canonical id in the egraph for the subject, and compute the
    // set of equivalent ids (i.e., ids found to be equivalent to the subject)
    let canonical_id = egraph.find(Id::from(id as usize));
    let equiv_ids = rev_can_id_lookup
        .entry(canonical_id)
        .or_insert_with(HashSet::default);
    equiv_ids.insert(Id::from(0 as usize));

    for m in &subj.mutants {
        // PEG id
        let id: u32 = m.code.parse().unwrap();
        let canonical_id = egraph.find(Id::from(id as usize));

        let equiv_ids = rev_can_id_lookup
            .entry(canonical_id)
            .or_insert_with(HashSet::default);
        equiv_ids.insert(Id::from(m.id as usize));
    }

    for can_id in rev_can_id_lookup.keys() {
        let equiv_ids = rev_can_id_lookup.get(can_id).unwrap();
        let n = equiv_ids.len() as u32;
        num_equivalences += n - 1;

        let equiv_class_as_string: String = itertools::sorted(equiv_ids)
            .iter()
            .map(|id| usize::from(**id).to_string())
            .intersperse(" ".to_string())
            .collect();
        if n > 1 {
            println!("-> {}", equiv_class_as_string);
        }
        if equiv_file.write_all(format!("{}\n", equiv_class_as_string).as_bytes()).is_err() {
            println!("Error writing to file!");
        }
    }
    num_equivalences
}

pub fn to_sexp_string(expr: &RecExpr<Peg>, i: usize) -> String {
    let expr_ref = expr.as_ref();
    let node = expr_ref.get(i).unwrap();
    let op = node.display_op().to_string();
    if node.is_leaf() {
        op
    } else {
        let mut vec = vec![op];
        node.for_each(|id| vec.push(to_sexp_string(expr, id.into())));
        format!("({})", vec.join(" "))
    }
}
