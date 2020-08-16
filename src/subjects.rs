use egg::*;
use serde::Deserialize;
use serde_aux::prelude::*;
use serde_xml_rs::from_reader;
use crate::peg::*;

/// `Subjects` represents a collection of methods and associated mutants for
/// Cornelius to run on as well as associated lookup information.
#[derive(Debug, Deserialize)]
pub struct Subjects {

  /// The subjects that Cornelius will run on.
  #[serde(rename = "subject", default)]
  pub subjects: Vec<Subject>,

  /// A `Vec` of IdTableEntries that represent the raw parsed id table
  pub id_table: IdTable,
}

impl Subjects {

  /// Compute a `RecExpr` from `self.id_table`, storing it in
  /// `self.rec_expr`.
  pub fn compute_rec_expr(&self) -> Result<RecExpr<Peg>, String> {
    let mut rec_expr = RecExpr::default();
    //println!("Computing rec_expr from Subjects");
    //println!("Length of id_table is {}", self.id_table.entries.len());
    for entry in self.id_table.entries.iter() {
      let peg: Peg = parse_peg_from_string(entry.peg.clone())?;
      //println!("Adding entry {} as peg {:?}", entry.peg, peg);
      rec_expr.add(peg);
    }
    Ok(rec_expr)
  }
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
  } else {
    &peg_str
  };

  let split_peg_str: Vec<&str> = peg_str.split(' ').collect();
  let op = split_peg_str.get(0).expect("Empty peg application");
  let children: Vec<_> = split_peg_str[1..]
    .iter()
    .map(|s|
        Id::from(s.parse::<u32>()
                  .unwrap_or_else(|_| panic!(format!("Couldn't parse u32 {}", s)))
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

  #[serde(rename = "egg")]
  /// The source code in an egg-readable format
  pub code: String,

  #[serde(rename = "mutant")]
  /// Each of the mutants associated with this subject
  pub mutants: Vec<Mutant>,
}

impl Subject {
  /// Create a new `Subject`
  pub fn make(source_file: String, method: String, inputs: &[(u32, &str)]) -> Subject {
    let mut mutants = Vec::new();

    for (id, src) in inputs[1..].iter() {
      let m = Mutant {
        id: *id,
        code: (*src).parse().unwrap(),
      };
      mutants.push(m);
    }

    Subject {
      source_file,
      method,
      code: String::from(inputs[0].1).parse().unwrap(),
      mutants,
    }
  }

  pub fn from_file(path: String) -> Result<Subjects, String> {
    use std::fs;
    println!("Reading from path {}", path);
    let contents = fs::read_to_string(path).unwrap();
    //println!("contents: {}", contents);
    let subjects: Subjects = from_reader(contents.as_bytes()).unwrap();
    Ok(subjects)
  }

}

#[derive(Debug, Deserialize, Hash, Eq, PartialEq, Clone)]
pub struct Code {
  #[serde(rename = "$value")]
  pub source: String,
}

impl Code {
  pub fn expr(&self) -> RecExpr<Peg> {
    self.source.parse().unwrap()
  }
  pub fn new(source: String) -> Code {
    Code { source }
  }
}

#[derive(Debug, Deserialize)]
pub struct Mutant {
  #[serde(deserialize_with = "deserialize_number_from_string")]
  pub id: u32,
  #[serde(rename = "egg")]
  pub code: String,
}
