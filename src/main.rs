pub mod expr;
pub mod delta;

use egg::*;
use crate::expr::{RewriteSystem, Subject, Code, EGraph};
use std::collections::{HashMap, HashSet};
use std::env;
use std::fs::File;
use std::io::prelude::*;

//use egg;

fn main() -> std::io::Result<()> {
    let rules = crate::expr::rw_rules();
    let args: Vec<String> = env::args().collect();
    let subject_files = &args[1..];

    // read subject from a file
    //let _subject_from_file = Subject::from_file("xmls/more_real_subjects.xml".to_string());
    //println!("is_err? {}", _subject_from_file.is_err());

    let mut total_num_equivs = 0;
    for subj_file in subject_files {
        let equiv_file = File::create("equiv-classes")?;
        let subj_file = subj_file.trim();
        let mut num_equivs = 0;
        println!("Running on subject file {}", subj_file);

        for (i, subj) in Subject::from_file(subj_file.to_string())
            .expect("Error reading subjects")
            .iter()
            .enumerate()
        {
            println!("---------------------------------------");
            println!("Running on Subject {}", i + 1);
            num_equivs += run_on_subject(subj, &rules, &equiv_file);
            println!("---------------------------------------");
        }
        println!("    Found a total of {} equivalences in file", num_equivs);
        total_num_equivs += num_equivs;
    }
    println!("Found a total of {} equivalences", total_num_equivs);
    Ok(())
}

/// Run on a subject and return number of identified equivalences.
/// This is defined to be the sum over each equivalence class C
/// the size |C| - 1:
///     sum_{C in Equiv Classes} |C| - 1
fn run_on_subject(subj: &Subject, rules: &RewriteSystem, mut equiv_file: &File) -> u32 {
    println!("Running on subject {}:{}", subj.source_file, subj.method);

    let mut num_equivalences = 0;

    // mid_to_egg_id maps mutant_ids to egraph ids
    let mut mid_to_egg_id = HashMap::<u32, Id>::default();

    // egg_id_to_mid maps egg-ids to mutant ids
    // let mut egg_id_to_mid = HashMap::<Id, u32>::default();

    // Map mutant ids to their corresponding mutants
    let mut mid_to_code = HashMap::<u32, &Code>::default();

    // Create an EGraph and add the original code
    let mut egraph = EGraph::default();
    let id = egraph.add_expr(&subj.code.expr());

    mid_to_egg_id.insert(0, id);

    // Populate lookup tables
    for m in &subj.mutants {
        let id = egraph.add_expr(&m.code.expr());
        mid_to_egg_id.insert(m.id, id);
        // egg_id_to_mid.insert(id, m.id);
        mid_to_code.insert(m.id, &m.code);
    }

    let runner = Runner::default()
        .with_egraph(egraph)
        .run(rules);

    let mut rev_can_id_lookup = HashMap::<Id, HashSet<Id>>::default();

    let egg_id = *mid_to_egg_id.get(&0).unwrap();
    let canonical_id = runner.egraph.find(egg_id);
    let equiv_ids = rev_can_id_lookup
        .entry(canonical_id)
        .or_insert_with(HashSet::default);
    equiv_ids.insert(0);
    // equiv_ids.insert(egg_id);
    // egg_id_to_mid.insert(egg_id, 0);
    mid_to_code.insert(0, &subj.code);

    for m in &subj.mutants {
        let egg_id = *mid_to_egg_id.get(&m.id).unwrap();
        let canonical_id = runner.egraph.find(egg_id);
        let equiv_ids = rev_can_id_lookup
            .entry(canonical_id)
            .or_insert_with(HashSet::default);
        // equiv_ids.insert(egg_id);
        equiv_ids.insert(m.id);
    }

    for can_id in rev_can_id_lookup.keys() {
        let equiv_ids = rev_can_id_lookup.get(can_id).unwrap();
        let n = equiv_ids.len() as u32;
        num_equivalences += n - 1;
        for mid in itertools::sorted(equiv_ids) {
            if n > 1 {
                print!("{} ", mid);
            }
            if equiv_file.write_all(format!("{} ", mid).as_bytes()).is_err() {
                println!("Error writing to file!");
            }
        }
        if n > 1 {
            println!();
        }
        if equiv_file.write_all(b"\n").is_err() {
            println!("Error writing to file!");
        }
    }
    num_equivalences
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::expr::{Peg, VarAnalysis};
    use crate::delta::dd;
    #[test]
    fn eq_refine_const_pass() {
        let start = "(phi (== (var a) 5) (var a) 5)";
        let end = "5";
        assert!(test_straight_rewrite(start, end));
    }

    #[test]
    fn eq_refine_var_trigger() {
        assert!(test_straight_rewrite("(phi (== (var a) (var b)) (var a) (var b))", "(var b)"));
    }

    #[test]
    fn eq_refine_var_pass() {
        assert!(test_straight_rewrite("(phi (== (var a) (var b)) (var c) (var c))", "(var c)"));
    }

    #[test]
    fn push_plus_over_phi_1() {
        assert!(test_straight_rewrite("(+ (phi (var a) 3 4) 1)", "(phi (var a) 4 5)"));
    }

    #[test]
    fn push_plus_over_phi_2() {
        assert!(test_straight_rewrite("(+ (phi (var a) (phi (var b) 1 2) 3) 1)", "(phi (var a) (phi (var b) 2 3) 4)"));
    }

    #[test]
    fn push_ge_over_phi_1() {
        assert!(test_straight_rewrite("(>= (phi (var a) (phi (var b) 0 2) 3) 1)", "false"));
    }

    #[test]
    fn push_ge_over_phi_2() {
        assert!(test_straight_rewrite("(>= (phi (var a) (phi (var b) 1 2) 3) 1)", "true"));
    }

    #[test]
    fn push_ge_over_phi_3() {
        assert!(test_straight_rewrite("(>=  2 (phi (var a) (phi (var b) 0 2) 3) )", "(var a)"));
    }

    #[test]
    fn push_ge_over_phi_4() {
        assert!(test_straight_rewrite("(>= 3 (phi (var a) (phi (var b) 1 2) 3))", "true"));
    }

    #[test]
    fn push_le_over_phi_1() {
        assert!(test_straight_rewrite("(<= 1 (phi (var a) (phi (var b) 0 2) 3))", "false"));
    }

    #[test]
    fn push_le_over_phi_2() {
        assert!(test_straight_rewrite("(<= 1 (phi (var a) (phi (var b) 1 2) 3))", "true"));
    }

    #[test]
    fn push_le_over_phi_3() {
        assert!(test_straight_rewrite("(<= (phi (var a) (phi (var b) 0 2) 3) 2)", "(var a)"));
    }

    #[test]
    fn push_le_over_phi_4() {
        assert!(test_straight_rewrite("(<= (phi (var a) (phi (var b) 1 2) 3) 3)", "true"));
    }

    #[test]
    fn push_gt_over_phi_1() {
        assert!(test_straight_rewrite("(> (phi (var a) (phi (var b) 1 2) 3) 1)", "false"));
    }

    #[test]
    fn push_gt_over_phi_2() {
        assert!(test_straight_rewrite("(> (phi (var a) (phi (var b) 1 2) 3) 0)", "true"));
    }

    #[test]
    fn push_gt_over_phi_3() {
        assert!(test_straight_rewrite("(>  3 (phi (var a) (phi (var b) 0 2) 3) )", "false"));
    }

    #[test]
    fn push_gt_over_phi_4() {
        assert!(test_straight_rewrite("(> 4 (phi (var a) (phi (var b) 1 2) 3))", "true"));
    }

    #[test]
    fn push_lt_over_phi_1() {
        assert!(test_straight_rewrite("(< 1 (phi (var a) (phi (var b) 1 2) 3))", "false"));
    }

    #[test]
    fn push_lt_over_phi_2() {
        assert!(test_straight_rewrite("(< 1 (phi (var a) (phi (var b) 2 3) 4))", "true"));
    }

    #[test]
    fn push_lt_over_phi_3() {
        assert!(test_straight_rewrite("(< (phi (var a) (phi (var b) 0 2) 3) 3)", "false"));
    }

    #[test]
    fn push_lt_over_phi_4() {
        assert!(test_straight_rewrite("(< (phi (var a) (phi (var b) 1 2) 3) 4)", "true"));
    }

    #[test]
    fn eq_propagation_1() {
        assert!(test_straight_rewrite("(== 0 0)", "true"));
    }

    #[test]
    fn eq_propagation_2() {
        assert!(test_straight_rewrite("(== 1 0)", "false"));
    }

    #[test]
    fn eq_propagation_3() {
        assert!(test_straight_rewrite("(== true  true )", "true"));
    }

    #[test]
    fn eq_propagation_4() {
        assert!(test_straight_rewrite("(== false false)", "true"));
    }

    #[test]
    fn eq_propagation_5() {
        assert!(test_straight_rewrite("(== 1  (phi (var c) 1 2))", "(phi (var c) true false)"));
    }

    #[test]
    fn eq_propagation_6() {
        assert!(!test_straight_rewrite("(== 1  (phi (var c) 1 2))", "true"));
    }

    #[test]
    fn eq_propagation_7() {
        assert!(test_straight_rewrite("(== 1  (phi (var c) 1 2))", "(var c)"));
    }

    #[test]
    fn eq_propagation_8() {
        assert!(test_straight_rewrite("(== 1  (phi (var c) 2 1))", "(! (var c))"));
    }

    #[test]
    fn eq_propagation_9() {
        assert!(test_straight_rewrite("(== 2  (phi (var c) 2 1))", "(var c)"));
    }


    #[test]
    fn neq_propagation_1() {
        assert!(test_straight_rewrite("(!= 0 0)", "false"));
    }

    #[test]
    fn neq_propagation_2() {
        assert!(test_straight_rewrite("(!= 1 0)", "true"));
    }

    #[test]
    fn neq_propagation_3() {
        assert!(test_straight_rewrite("(!= true  true )", "false"));
    }

    #[test]
    fn neq_propagation_4() {
        assert!(test_straight_rewrite("(!= false false)", "false"));
    }

    #[test]
    fn neq_propagation_5() {
        assert!(test_straight_rewrite("(!= 1  (phi (var c) 1 2))", "(phi (var c) false true)"));
        assert!(test_straight_rewrite("(!= 1  (phi (var c) 1 2))", "(! (var c))"));
    }

    #[test]
    fn neq_propagation_6() {
        assert!(!test_straight_rewrite("(!= 1  (phi (var c) 1 2))", "true"));
    }

    #[test]
    fn neq_propagation_7() {
        assert!(!test_straight_rewrite("(!= 1  (phi (var c) 1 2))", "false"));
    }

    #[test]
    fn neq_propagation_8() {
        assert!(!test_straight_rewrite("(!= 1  (phi (var c) 1 2))", "1"));
    }

    fn test_straight_rewrite(start: &str, end: &str) -> bool {

        let start_expr = start.parse().unwrap();
        let end_expr = end.parse().unwrap();
        let mut eg = EGraph::default();
        eg.add_expr(&start_expr);
        let rules: Box<RewriteSystem> = crate::expr::rw_rules();
        let runner = Runner::default()
            .with_egraph(eg)
            .run(rules.into_iter());
        !runner.egraph.equivs(&start_expr, &end_expr).is_empty()
    }

    /// Test if the current rewrite rules allow for start to be written to end,
    /// and if so report error and a minimal subset of rewrite rules that allow
    /// for this erroneous rewrite sequence to happen.
    fn test_straight_no_rewrite(start: &str, end: &str) -> bool {

        if test_straight_rewrite(start, end) {
            let rules = crate::expr::rw_rules();
            let start_expr = start.parse().unwrap();
            let end_expr = end.parse().unwrap();
            let oracle: dyn FnMut (_) -> bool =
                move |config: &[&Rewrite<Peg,VarAnalysis>]| {
                    let mut eg = EGraph::default();
                    eg.add_expr(&start_expr);
                    let runner = Runner::default()
                        .with_egraph(eg)
                        .run(config.iter());
                    runner.egraph.equivs(&start_expr, &end_expr).is_empty()
                };
            let rules: Vec<_> = rules.iter().collect();
            let min_config = dd(&rules, oracle);
            //for (i, rule) in min_config.iter().enumerate() {
            //    println!("{}) {}", i+1, rule.name());
            //}
            return false;
        }
        true
    }
}
