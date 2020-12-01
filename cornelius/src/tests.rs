use egg::*;
use crate::rewrites::RewriteSystem;
use crate::peg::{Peg, VarAnalysis};
use crate::util::EqRel;
use std::env;

use crate::delta::dd;
use std::time::Instant;

use std::fs::{read_to_string};

#[cfg(test)]
mod const_prop {
    use super::*;

    #[test]
    fn add() {
        assert!(ensure_const_prop_i32("(+ 1 2)", 3));
        assert!(!ensure_const_prop_i32("(+ 1 2)", 4));
        assert!(ensure_const_prop_i32("(+ 1 (+ 2 3))", 6));
    }

    #[test]
    fn sub() {
        assert!(ensure_const_prop_i32("(- 1 2)", -1));
        assert!(ensure_const_prop_i32("(- 1 (- 2 3))", 1 - (2 - 3)));
    }

    #[test]
    fn mul() {
        assert!(ensure_const_prop_i32("(* 1 2)", 2));
        assert!(ensure_const_prop_i32("(* 1 (* 2 3))", 6));
        assert!(ensure_const_prop_i32("(* 10 (* 2 3))", 60));
        assert!(ensure_const_prop_i32("(* 11 (* 2 3))", 66));
        assert!(ensure_const_prop_i32("(* (* 4 5) (* 2 3))", 4*5*2*3));
        assert!(ensure_const_prop_i32("(* 0 2)", 0));
    }

    #[test]
    fn div() {
        assert!(ensure_const_prop_i32("(/ 1 2)", 0));
        assert!(ensure_const_prop_i32("(/ 4 2)", 2));
        assert!(ensure_is_error("(/ 4 0)"));
    }

    // The following tests ensure that Cornelius doesn't employ constant
    // propagation incorrectly.
    #[test]
    fn vars_eq() {
        assert!(test_no_straight_rewrite("(== (var a) (var b))", "false", &[]));
        assert!(test_no_straight_rewrite("(== (var a) (var b))", "true", &[]));
        assert!(test_no_straight_rewrite("(== 1 (var b))", "false", &[]));
        assert!(test_no_straight_rewrite("(== 1 (var b))", "true", &[]));
    }

    #[test]
    fn vars_ne() {
        assert!(test_no_straight_rewrite("(!= (var a) (var b))", "false", &[]));
        assert!(test_no_straight_rewrite("(!= (var a) (var b))", "true", &[]));
        assert!(test_no_straight_rewrite("(!= 1 (var b))", "false", &[]));
        assert!(test_no_straight_rewrite("(!= 1 (var b))", "true", &[]));
    }

    #[test]
    fn vars_gt() {
        assert!(test_no_straight_rewrite("(> (var a) (var b))", "false", &[]));
        assert!(test_no_straight_rewrite("(> (var a) (var b))", "true", &[]));
        assert!(test_no_straight_rewrite("(> 1 (var b))", "false", &[]));
        assert!(test_no_straight_rewrite("(> 1 (var b))", "true", &[]));
    }

    #[test]
    fn vars_gte() {
        assert!(test_no_straight_rewrite("(>= (var a) (var b))", "false", &[]));
        assert!(test_no_straight_rewrite("(>= (var a) (var b))", "true", &[]));
        assert!(test_no_straight_rewrite("(>= 1 (var b))", "false", &[]));
        assert!(test_no_straight_rewrite("(>= 1 (var b))", "true", &[]));
    }

    #[test]
    fn vars_lt() {
        assert!(test_no_straight_rewrite("(< (var a) (var b))", "false", &[]));
        assert!(test_no_straight_rewrite("(< (var a) (var b))", "true", &[]));
        assert!(test_no_straight_rewrite("(< 1 (var b))", "false", &[]));
        assert!(test_no_straight_rewrite("(< 1 (var b))", "true", &[]));
    }

    #[test]
    fn vars_lte() {
        assert!(test_no_straight_rewrite("(<= (var a) (var b))", "false", &[]));
        assert!(test_no_straight_rewrite("(<= (var a) (var b))", "true", &[]));
        assert!(test_no_straight_rewrite("(<= 1 (var b))", "false", &[]));
        assert!(test_no_straight_rewrite("(<= 1 (var b))", "true", &[]));
    }

    // Helper functions
    fn ensure_const_prop_i32(start: &str, expected: i32) -> bool {

        let start_expr = start.parse().unwrap();
        // let mut expected_expr: RecExpr<Peg> = RecExpr::default();
        //expected_expr.add(Peg::Num(expected));
        let mut egraph = EGraph::default();
        let id = egraph.add_expr(&start_expr);
        let rules: Box<RewriteSystem> = crate::rewrites::rw_rules();
        let runner = Runner::default()
            .with_egraph(egraph)
            .run(rules.iter());
        match runner.egraph[id].data.constant {
            Some(Peg::Num(result)) => expected == result,
            _ => false
        }
    }

    fn ensure_is_error(start: &str) -> bool {

        let start_expr = start.parse().unwrap();
        // let mut expected_expr: RecExpr<Peg> = RecExpr::default();
        //expected_expr.add(Peg::Num(expected));
        let mut egraph = EGraph::default();
        let id = egraph.add_expr(&start_expr);
        let rules: Box<RewriteSystem> = crate::rewrites::rw_rules();
        let runner = Runner::default()
            .with_egraph(egraph)
            .run(rules.iter());
        match runner.egraph[id].data.constant {
            Some(Peg::Error) => true,
            _ => false
        }
    }
}


#[cfg(test)]
mod propagate_equality {
    use super::*;

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
}

#[cfg(test)]
mod propagate_non_equality {
    use super::*;

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
        assert!(test_no_straight_rewrite("(!= 1  (phi (var c) 3 2))", "false", &[]));
    }

    #[test]
    fn neq_propagation_9() {
        assert!(test_straight_rewrite("(!= 1  (phi (var c) 3 2))", "true"));
    }
}

#[cfg(test)]
mod refine_equalities {
    use super::*;

    #[test]
    #[ignore]
    fn eq_refine_const_pass() {
        let start = "(phi (== (var a) 5) (var a) 5)";
        let end = "5";
        assert!(test_straight_rewrite(start, end));
    }

    #[test]
    #[ignore]
    fn eq_refine_var_trigger() {
        assert!(test_straight_rewrite("(phi (== (var a) (var b)) (var a) (var b))", "(var b)"));
    }

}


#[cfg(test)]
mod distribute_over_phi {
    use super::*;

    #[test]
    fn push_plus_over_phi_1() {
        assert!(test_straight_rewrite("(+ (phi (var a) 3 4) 1)", "(phi (var a) 4 5)"));
    }

    #[test]
    fn push_plus_over_phi_2() {
        assert!(test_straight_rewrite("(+ (phi (var a) (phi (var b) 1 2) 3) 1)", "(phi (var a) (phi (var b) 2 3) 4)"));
    }

    #[test]
    fn push_ge_over_phi_a() {
        assert!(test_straight_rewrite("(>= 1 (phi (var a) 1 1))", "true"));
    }

    #[test]
    fn push_ge_over_phi_b() {
        assert!(test_straight_rewrite("(>= 2 (phi (var a) 0 1))", "true"));
    }

    #[test]
    fn push_ge_over_phi_1() {
        assert!(test_straight_rewrite("(>= (phi (var a) (phi (var b) 0 2) 3) 4)", "false"));
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
        assert!(test_no_straight_rewrite("(<= 1 (phi (var a) (phi (var b) 0 2) 3))", "false", &[]));
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
        assert!(test_no_straight_rewrite("(> (phi (var a) (phi (var b) 1 2) 3) 1)", "false", &[]));
    }

    #[test]
    fn push_gt_over_phi_2() {
        assert!(test_straight_rewrite("(> (phi (var a) (phi (var b) 1 2) 3) 0)", "true"));
    }

    #[test]
    fn push_gt_over_phi_3() {
        assert!(test_straight_rewrite("(>  3 (phi (var a) (phi (var b) 0 1) 2) )", "true"));
    }

    #[test]
    fn push_gt_over_phi_4() {
        assert!(test_straight_rewrite("(> 4 (phi (var a) (phi (var b) 1 2) 3))", "true"));
    }

    #[test]
    fn push_gt_over_phi_5() {
        assert!(test_no_straight_rewrite("(> 4 (phi (var a) (phi (var b) 1 2) 4))", "true", &[]));
    }


    #[test]
    fn push_lt_over_phi_1() {
        assert!(test_straight_rewrite("(< 0 (phi (var a) (phi (var b) 1 2) 3))", "true"));
    }

    #[test]
    fn push_lt_over_phi_2() {
        assert!(test_straight_rewrite("(< 1 (phi (var a) (phi (var b) 2 3) 4))", "true"));
    }

    #[test]
    fn push_lt_over_phi_3() {
        assert!(test_straight_rewrite("(< (phi (var a) (phi (var b) 0 2) 3) 0)", "false"));
    }

    #[test]
    fn push_lt_over_phi_4() {
        assert!(test_straight_rewrite("(< (phi (var a) (phi (var b) 1 2) 3) 4)", "true"));
    }
}

#[cfg(test)]
mod misc {
    use super::*;

    #[test]
    fn test_max_1() {
        assert!(test_no_straight_rewrite("(phi (! (phi (> (var a) (var b)) true false)) (var b) (phi (> (var a) (var b)) (var a) -2147483648))", "(var b)", &[]))
    }

    #[test]
    fn test_max_2() {
        assert!(test_no_straight_rewrite("(phi (! (phi (!= (var a) (var b)) true false)) (var b) (phi (!= (var a) (var b)) (var a) -2147483648))", "(var b)", &[]))
    }

    #[test]
    fn test_max_3() {
        assert!(test_no_straight_rewrite("(phi (! (phi (>= (var a) (var b)) true false)) (var b) (phi (>= (var a) (var b)) (var a) -2147483648))", "(var b)", &[]))
    }

    #[test]
    fn test_max_4() {
        assert!(test_straight_rewrite("(phi (! (phi false true false)) (var b) (phi false (var a) -2147483648))", "(var b)"));
    }

    #[test]
    fn ensure_no_rewrite_1() {

        // The following are all just sanity checks. These all pass
        assert!(test_no_straight_rewrite("(phi (>= (var a) (var b)) true false)", "true" , &[]));
        assert!(test_no_straight_rewrite("(phi (>= (var a) (var b)) true false)", "false", &[]));
        assert!(test_no_straight_rewrite("(phi (<= (var a) (var b)) true false)", "true" , &[]));
        assert!(test_no_straight_rewrite("(phi (<= (var a) (var b)) true false)", "false", &[]));
        assert!(test_no_straight_rewrite("(phi (> a b) true false)"             , "true",  &[]));
        assert!(test_no_straight_rewrite("(phi (< a b) true false)"             , "true",  &[]));
        assert!(test_no_straight_rewrite("(phi (> (var a) (var b)) true false)" , "true" , &[]));
        assert!(test_no_straight_rewrite("(phi (< (var a) (var b)) true false)" , "true",  &[]));
        assert!(test_no_straight_rewrite("(phi (> a b) true false)"             , "false", &[]));
        assert!(test_no_straight_rewrite("(phi (< a b) true false)"             , "false", &[]));

    }

    #[test]
    fn ensure_no_rewrite_2() {
        assert!(test_no_straight_rewrite("(> (var a) (var b))", "false", &["(! (phi (> (var a) (var b)) true false))"]))
    }

    #[test]
    fn ensure_no_rewrite_3() {
        assert!(test_no_straight_rewrite("false", "true", &["(! (phi (> (var a) (var b)) true false))"]));
    }

    #[test]
    #[ignore]
    fn eq_distr_var_1() {
        // Ignored until equality distribution is restored
        assert!(test_straight_rewrite("(eq-distr (var a) (var b) (var c))", "(var c)"));
    }

    #[test]
    #[ignore]
    fn eq_distr_eq_1() {
        // Ignored until equality distribution is restored
        assert!(test_straight_rewrite("(eq-distr (var a) (var b) (== (var a) (var b)))", "true"));
    }

    #[test]
    fn add_ac_overflow(){
        // The following are regression tests to prevent against overflow bugs. More details here:
        // https://bkushigian.github.io/2020/08/10/debugging-rewrites-1.html
        assert!(rewrites_do_not_panic(&["(+ (phi C 1 0) 2)", "(phi true 0 -2147483648)"]));
        assert!(rewrites_do_not_panic(&["(+ (phi C 1 1) 2)", "(phi true 0 -2147483648)"]));
        assert!(rewrites_do_not_panic(&["(+ (phi C 1 1) 0)", "(phi true 0 -2147483648)"]));
        assert!(rewrites_do_not_panic(&["(+ 1 0)", "(phi true 0 -2147483648)"]));
        assert!(rewrites_do_not_panic(&["(+ (phi C 0 0) 0)", "(phi true 0 -2147483648)"]));
        assert!(rewrites_do_not_panic(&["(+ (phi C 1 0) 1)", "(phi true 0 -76650000)"]));
        assert!(rewrites_do_not_panic(&["(+ (phi C 1 0) 1)", "(phi true 0 -76700000)"]));
        assert!(rewrites_do_not_panic(&["(+ (phi C 1 0) 1)", "(phi true 0 -76695844)"]));
        assert!(rewrites_do_not_panic(&["(+ (phi C 1 0) 1)", "(phi true 0 -76695845)"]));
        assert!(rewrites_do_not_panic(&["(+ (phi C 0 0) 1)", "(phi true 1 -2147483648)"]));
        assert!(rewrites_do_not_panic(&["(+ (phi C 1 0) 1)", "(phi true 1 -2147483648)"]));
        assert!(rewrites_do_not_panic(&["(+ (phi true 0 -2147483647) 0)", "(phi true 0 -2147483647)"]));
    }

}

#[cfg(test)]
mod heap {
    use super::*;
    #[test]
    fn rd_wr() {
        assert!(test_straight_rewrite("(rd (path (var a) (derefs .a.b)) (wr (path (var a) (derefs .a.b)) 3 (heap 0 unit)))", "3"));
    }

    #[test]
    fn test_field_write_1() {
        assert!(test_straight_rewrite(
            // Original program
            // public class MyClass {
            //     int x = 0;
            //
            //     int test1(int a) {
            //         int result = 0;
            //         x = a - a;
            //         result = x;
            //         return result;
            //     }
            // }

            "(return-node (rd (path (var this) (derefs x)) (wr (path (var this) (derefs x)) (- (var a) (var a)) (heap 0 unit))) (wr (path (var this) (derefs x)) (- (var a) (var a)) (heap 0 unit)))",
            // Mutant
            // public class MyClass {
            //     int x = 0;
            //
            //     int test1(int a) {
            //         int result = 0;
            //         x = a - a;
            //         ;
            //         return result;
            //     }
            // }

            "(return-node 0 (wr (path (var this) (derefs x)) (- (var a) (var a)) (heap 0 unit)))"))
    }

    #[test]
    #[ignore]
    // The folowing test is IGNORED until Equality Refinement is implemented
    fn test_field_write_2() {
        assert!(test_straight_rewrite(
        // Original Program
        // public class MyClass
        //     int x = 0;
        //      int test_heapy_max(int a, int b) {
        //           x = a > b ? a : b;
        //           int result = x;
        //           return result;
        //      }
        // }
"(return-node
    (rd (path (var this) (derefs x))
        (wr (path (var this) (derefs x))
            (phi (> (var a) (var b)) (var a) (var b))
            (heap 0 unit)))
    (wr (path (var this) (derefs x))
        (phi (> (var a) (var b)) (var a) (var b))
        (heap 0 unit)))",
        // Mutant Program
        // public class MyClass
        //     int x = 0;
        //      int test_heapy_max(int a, int b) {
        //           x = a >= b ? a : b;
        //           int result = x;
        //           return result;
        //      }
        // }
"(return-node
    (rd (path (var this) (derefs x))
        (wr (path (var this) (derefs x))
            (phi (>= (var a) (var b)) (var a) (var b))
            (heap 0 unit)))
    (wr (path (var this) (derefs x))
        (phi (>= (var a) (var b)) (var a) (var b))
        (heap 0 unit)))"))
    }
}

#[cfg(test)]
mod serialization {
    use super::*;
    #[test]
    fn field_write_gt() {
        ensure_serialized_subject_meets_specs("../tests/field-write.xml", "../tests/field-write.gt", 1);
    }

    #[test]
    fn field_write_deduplication() {
        ensure_no_duplicates_in_serialized("../tests/field-write.xml");
    }

    #[test]
    fn field_access_gt() {
        ensure_serialized_subject_meets_specs("../tests/field-access.xml", "../tests/field-access.gt", 1);
    }

    #[test]
    fn field_access_deduplication() {
        ensure_no_duplicates_in_serialized("../tests/field-access.xml");
    }

    #[test]
    fn method_invocation_gt() {
        ensure_serialized_subject_meets_specs("../tests/method-invocation.xml", "../tests/method-invocation.gt", 1);
    }

    #[test]
    fn method_invocation_deduplication() {
        ensure_no_duplicates_in_serialized("../tests/method-invocation.xml");
    }
}

#[allow(dead_code)]
fn test_straight_rewrite(start: &str, end: &str) -> bool {
    exprs_collide(start, end, &[])
}

/// exprs_collide
///
/// Ensure that expressions `e1` and `e2` do not become equal during equality
/// saturation when expressions `others` are in the egraph
#[allow(dead_code)]
fn exprs_collide(e1: &str, e2: &str, others: &[&str]) -> bool {
    let e1 = e1.parse().unwrap();
    let e2 = e2.parse().unwrap();
    let mut eg = EGraph::default();
    eg.add_expr(&e1);
    eg.add_expr(&e2);
    for e in others {
        eg.add_expr(&e.parse().unwrap());
    }
    let rules: Box<RewriteSystem> = crate::rewrites::rw_rules();
    let runner = Runner::default()
        .with_egraph(eg)
        .run(rules.iter());
    !runner.egraph.equivs(&e1, &e2).is_empty()
}

#[allow(dead_code)]
/// Test if the current rewrite rules allow for start to be written to end,
/// and if so report error and a minimal subset of rewrite rules that allow
/// for this erroneous rewrite sequence to happen.
fn test_no_straight_rewrite(start: &str, end: &str, other: &[&str]) -> bool {

    let run_dd = env::var("CORNELIUS_MINIMIZE_RULES").is_ok();
    let debug = match env::var("CORNELIUS_DEBUG") {
        Ok(s) => match &s[..] {
            "1" => true,
            _ => false
        }
        _ => false
    };

    if debug {
        println!("debug=true");
    }

    let mut trial: u32 = 0;

    if exprs_collide(start, end, other) {
        if  run_dd {
            let start_expr = start.parse().unwrap();
            let end_expr = end.parse().unwrap();
            let oracle =
                move |config: &[&Rewrite<Peg,VarAnalysis>]| -> bool {
                    if debug {
                        println!("Trial {}: running oracle on len {} config", trial, config.len());
                        trial += 1;
                    }
                    let mut eg = EGraph::default();
                    eg.add_expr(&start_expr);
                    eg.add_expr(&end_expr);
                    for o in other {
                        let expr = o.parse().unwrap();
                        eg.add_expr(&expr);
                    }
                    let runner = Runner::default()
                        .with_egraph(eg)
                        .run(config.iter().cloned());
                    let not_equiv = runner.egraph.equivs(&start_expr, &end_expr).is_empty();
                    if debug && ! not_equiv {
                        println!("    Found new minimal config");
                        for (i, rule) in config.iter().enumerate() {
                            println!("    ({}) {}  {}", i+1, rule.name(), rule.long_name());
                        }
                        println!("--------------------------------------------------------------------------------");
                    }
                    not_equiv
                };

            let rules = crate::rewrites::rw_rules();
            let rules: Vec<_> = rules.iter().collect();
            let min_config = dd(&rules, oracle);
            let dd_start_time = Instant::now();

            for (i, rule) in min_config.iter().enumerate() {
                println!("({}) {}  {}", i+1, rule.name(), rule.long_name());
            }
            if debug {
                let seconds = dd_start_time.elapsed().as_secs();
                println!("Minimized buggy input from {} rules to {} rules in {} seconds",
                            rules.len(),
                            min_config.len(),
                            seconds
                );
            }
        }
        return false;
    }
    true
}


#[allow(dead_code)]
fn rewrites_do_not_panic(exprs: &[&str]) -> bool{
    let mut egraph: EGraph<Peg, VarAnalysis> = EGraph::default();
    let rules = crate::rewrites::rw_rules();
    let runner = Runner::default();
    for expr in exprs {
        egraph.add_expr(&expr.parse().unwrap());
    }
    runner.with_egraph(egraph).run(rules.iter());

    true
}

/// Read in a serialized subject and run Cornelius on it. Check it against the
/// ground truth file, ensuring there are no soundness violations. Finally,
/// ensure that Cornelius detected _at least_ `min_score` equivalences.
#[allow(dead_code)]
fn ensure_serialized_subject_meets_specs(
    subjects_file: &str,
    ground_truth_file: &str,
    min_score: u32
) {

    let subjects = crate::subjects::run_on_subjects_file(subjects_file).unwrap();
    let mut score = 0;
    for subject in subjects.subjects {
        let eq_rel = EqRel::from(&subject.analysis_result);
        let gt_rel: EqRel<u32> = read_to_string(ground_truth_file)
            .map(|cs| EqRel::from(cs.as_str()))
            .unwrap();

        assert!(eq_rel.is_refinement_of(&gt_rel),
                format!("{:?} does not refine {:?}", eq_rel, gt_rel));
        score += subject.analysis_result.score;
    }
    assert!(score >= min_score, format!("score = {} < min_score = {}", score, min_score));
}

/// Read in a serialized file, parse it into a subjects, and add it to an
/// EGraph. Then, check that the size of the EGraph is correct (i.e., that there
/// were no duplications of syntactically identical pegs from serialization).
#[allow(dead_code)]
fn ensure_no_duplicates_in_serialized(
    subjects_file: &str
) {
    let subjects: crate::subjects::Subjects = crate::subjects::Subject::from_file(subjects_file.to_string()).unwrap();
    let rec_expr = subjects.compute_rec_expr().unwrap();
    let rec_expr_ref = rec_expr.as_ref();
    let mut egraph = EGraph::<Peg,()>::default();

    for i in 0..rec_expr_ref.len(){
        let new_id = egraph.add(rec_expr_ref[i].clone());

        let egg_size = egraph.total_size();
        let v = rec_expr_ref[0..(i+1)].to_vec();
        let re = RecExpr::from(v);
        assert!((i + 1) == egg_size, format!("rec_expr_size: {} != egraph size: {}\nre: `{}`", i + 1, egg_size, re.pretty(80)));
        assert!(usize::from(new_id) == i, format!("id: {}, i: {}", usize::from(new_id), i));
    }

    let mut egraph = EGraph::<Peg,()>::default();
    egraph.add_expr(&rec_expr);
    let egg_size = egraph.total_size();
    let ser_size = rec_expr.as_ref().len();
    assert!(ser_size == egg_size, format!("serialized size: {} != egraph size: {}", ser_size, egg_size));

}
