use egg::*;
use crate::rewrites::RewriteSystem;
use crate::peg::{Peg, VarAnalysis};
use std::env;

use crate::delta::dd;
use std::time::Instant;

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

        // These are very much like the failing assertions below, only with
        // `true` and `false` swapped out to `7` and `8` respectively. These do
        // not fail. This is interesting for the following two reasons:
        //
        // 1. If any of the first 3 assertions failed, this would mean that the
        //    bug stemmed from the rewrite rules erroneously selecting the
        //    `else` argument from the phi node. This is what I was expecting.
        //
        //    For instance, if
        //
        //        assert!(test_no_straight_rewrite("(phi (> (var a) (var b)) 7 8)" , "8", &[]));
        //
        //    failed, then this would indicate that `8` was being selected
        //    from the `then` clause of the `phi`.
        //
        // 2. If any of the next 3 assertiosn failed, this would indicate that
        //    bug stemmed from the rewrite rules erroneously transforming a phi
        //    node into `false`.
        //
        //    For instance, if
        //
        //        assert!(test_no_straight_rewrite("(phi (> (var a) (var b)) 7 8)" , "false", &[]));
        //
        //    failed, this would mean that the entire phi node was being rewritten
        //    to `false`, which would surprise me greatly, since types
        //    wouldn't be preserved.
        //
        //  Since neither of the following groups fail, this implies that the
        //  failure is some combination of rewriting the entire phi node and
        //  relying on the arguments
        assert!(test_no_straight_rewrite("(phi (> 3 2) 7 8)" , "8", &[]));
        assert!(test_no_straight_rewrite("(phi (> (var a) (var b)) 7 8)" , "8", &[]));
        assert!(test_no_straight_rewrite("(phi (< (var a) (var b)) 7 8)" , "8", &[]));

        assert!(test_no_straight_rewrite("(phi (> 3 2) 7 8)" , "false", &[]));
        assert!(test_no_straight_rewrite("(phi (> (var a) (var b)) 7 8)" , "false", &[]));
        assert!(test_no_straight_rewrite("(phi (< (var a) (var b)) 7 8)" , "false", &[]));
        assert!(test_no_straight_rewrite("(phi (> 3 2) false true)" , "true" , &[]));
        assert!(test_no_straight_rewrite("(phi (> (var a) (var b)) false true)" , "true" , &[]));
        assert!(test_no_straight_rewrite("(phi (> 3 2) true false)" , "false", &[]));
        assert!(test_no_straight_rewrite("(phi (> (var a) (var b)) true false)" , "false", &[]));
        assert!(test_no_straight_rewrite("(phi (< (var a) (var b)) true false)" , "false", &[]));
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
    fn eq_distr_var_1() {
        //assert!(test_straight_rewrite("(eq-distr (var a) (var b) (var c))", "(var c)"));
    }

    #[test]
    fn eq_distr_eq_1() {
        //assert!(test_straight_rewrite("(eq-distr (var a) (var b) (== (var a) (var b)))", "true"));
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
        assert!(test_straight_rewrite("(rd (path (var a) (derefs .a.b)) (wr (path (var a) (derefs .a.b)) 3 (heap 0)))", "3"));
    }
}

#[allow(dead_code)]
fn test_straight_rewrite(start: &str, end: &str) -> bool {
    let start_expr = start.parse().unwrap();
    let end_expr = end.parse().unwrap();
    let mut eg = EGraph::default();
    eg.add_expr(&start_expr);
    let rules: Box<RewriteSystem> = crate::rewrites::rw_rules();
    let runner = Runner::default()
        .with_egraph(eg)
        .run(rules.iter());
    !runner.egraph.equivs(&start_expr, &end_expr).is_empty()
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

    if test_straight_rewrite(start, end) {
        if  run_dd {
            let dd_start_time = Instant::now();
            let rules = crate::rewrites::rw_rules();
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
            let rules: Vec<_> = rules.iter().collect();
            let min_config = dd(&rules, oracle);

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
