use crate::config::RunConfig;
use crate::global_data::GlobalData;
use crate::peg::{Peg, PegAnalysis};
use crate::rewrites::RewriteSystem;
use crate::util::EqRel;
use egg::*;
use std::env;
use crate::primitives::{JavaInt, JavaLong};

use crate::delta::dd;
use std::time::Instant;

use std::fs::read_to_string;

#[cfg(test)]
mod parse {
    fn parse_a_peg(expr: &str) -> Option<Peg> {
        let expr: Result<RecExpr<Peg>, String> = expr.parse();
        if let Ok(expr) = expr {
            let expr_ref = expr.as_ref();
            return Some(expr_ref[expr_ref.len() - 1].clone());
        }

        None
    }

    use super::*;

    #[test]
    fn parse_long_const() {
        assert!(parse_a_peg("0l") == Some(Peg::Long(JavaLong::from(0))));
        assert!(parse_a_peg("1000l") == Some(Peg::Long(JavaLong::from(1000))));
    }

    #[test]
    fn parse_int_const() {
        assert!(parse_a_peg("0") == Some(Peg::Num(JavaInt::from(0))));
        assert!(parse_a_peg("1000") == Some(Peg::Num(JavaInt::from(1000))));
    }

    #[test]
    fn parse_instanceof() {
        assert!(try_to_parse("(instanceof 1 2)"));
        assert!(try_to_parse("(instanceof (var (key) (nil)) \"Serializable\")"));
    }

}
#[cfg(test)]
mod const_prop {
    use super::*;

    #[test]
    fn add() {
        assert!(ensure_const_prop("(+ 1 2)", Peg::Num(JavaInt::from(3))));
        assert!(!ensure_const_prop("(+ 1 2)", Peg::Num(JavaInt::from(4))));
        assert!(ensure_const_prop("(+ 1 (+ 2 3))", Peg::Num(JavaInt::from(6))));

        assert!(ensure_const_prop("(+ 1l (+ 2 3))", Peg::Long(JavaLong::from(6))));
        assert!(ensure_const_prop("(+ 1 (+ 2l 3))", Peg::Long(JavaLong::from(6))));
    }

    #[test]
    fn sub() {
        assert!(ensure_const_prop("(- 1 2)", Peg::Num(JavaInt::from(-1))));
        assert!(ensure_const_prop("(- 1 (- 2 3))", Peg::Num(JavaInt::from(1 - (2 - 3)))));

        assert!(ensure_const_prop("(- 1l (+ 2 3))", Peg::Long(JavaLong::from(1 - (2 + 3)))));
        assert!(ensure_const_prop("(- 1 (+ 2 3l))", Peg::Long(JavaLong::from(1 - (2 + 3)))));
    }

    #[test]
    fn mul() {
        assert!(ensure_const_prop("(* 1 2)", Peg::Num(JavaInt::from(2))));
        assert!(ensure_const_prop("(* 1 (* 2 3))", Peg::Num(JavaInt::from(6))));
        assert!(ensure_const_prop("(* 10 (* 2 3))", Peg::Num(JavaInt::from(60))));
        assert!(ensure_const_prop("(* 11 (* 2 3))", Peg::Num(JavaInt::from(66))));
        assert!(ensure_const_prop("(* (* 4 5) (* 2 3))",Peg::Num(JavaInt::from(4 * 5 * 2 * 3)) ));
        assert!(ensure_const_prop("(* 0 2)", Peg::Num(JavaInt::from(0))));

        assert!(ensure_const_prop("(* (* 4 5L) (* 2l 3))", Peg::Long(JavaLong::from(4 * 5 * 2 * 3))));
    }

    #[test]
    fn div() {
        assert!(ensure_const_prop("(/ 1 2)", Peg::Num(JavaInt::from(0))));
        assert!(ensure_const_prop("(/ 4 2)", Peg::Num(JavaInt::from(2))));
        assert!(ensure_is_error("(/ 4 0)"));

        assert!(ensure_const_prop("(/ 4l 2)",  Peg::Long(JavaLong::from(2))));
        assert!(ensure_const_prop("(/ 4 2l)",  Peg::Long(JavaLong::from(2))));
        assert!(ensure_const_prop("(/ 4L 2l)", Peg::Long(JavaLong::from(2))));
    }

    #[test]
    fn bin_or() {
        assert!(ensure_const_prop("(| 2 4)", Peg::Num(JavaInt::from(6))));
        assert!(ensure_const_prop("(| 2 3)", Peg::Num(JavaInt::from(3))));
    }

    #[test]
    fn bin_xor() {
        assert!(ensure_const_prop("(^ 2 4)", Peg::Num(JavaInt::from(6))));
        assert!(ensure_const_prop("(^ 1 3)", Peg::Num(JavaInt::from(2))));
    }

    #[test]
    fn eq() {
        assert!(ensure_const_prop("(== 0 0)", Peg::Bool(true)));
        assert!(ensure_const_prop("(== 0l 0l)", Peg::Bool(true)));
        assert!(JavaInt::from(0) == JavaLong::from(0));
        assert!(JavaLong::from(0) == JavaInt::from(0));
        assert!(ensure_const_prop("(== 0l 0)", Peg::Bool(true)));
        assert!(ensure_const_prop("(== 0 0l)", Peg::Bool(true)));
    }

    // The following tests ensure that Cornelius doesn't employ constant
    // propagation incorrectly.
    #[test]
    fn vars_eq() {
        assert!(test_no_straight_rewrite(
            "(== (var a nil) (var b nil))",
            "false",
            &[]
        ));
        assert!(test_no_straight_rewrite(
            "(== (var a nil) (var b nil))",
            "true",
            &[]
        ));
        assert!(test_no_straight_rewrite("(== 1 (var b nil))", "false", &[]));
        assert!(test_no_straight_rewrite("(== 1 (var b nil))", "true", &[]));
    }

    #[test]
    fn vars_ne() {
        assert!(test_no_straight_rewrite(
            "(!= (var a nil) (var b nil))",
            "false",
            &[]
        ));
        assert!(test_no_straight_rewrite(
            "(!= (var a nil) (var b nil))",
            "true",
            &[]
        ));
        assert!(test_no_straight_rewrite("(!= 1 (var b nil))", "false", &[]));
        assert!(test_no_straight_rewrite("(!= 1 (var b nil))", "true", &[]));
    }

    #[test]
    fn vars_gt() {
        assert!(test_no_straight_rewrite(
            "(> (var a nil) (var b nil))",
            "false",
            &[]
        ));
        assert!(test_no_straight_rewrite("(> (var a nil) (var b nil))", "true", &[]));
        assert!(test_no_straight_rewrite("(> 1 (var b nil))", "false", &[]));
        assert!(test_no_straight_rewrite("(> 1 (var b nil))", "true", &[]));
    }

    #[test]
    fn vars_gte() {
        assert!(test_no_straight_rewrite(
            "(>= (var a nil) (var b nil))",
            "false",
            &[]
        ));
        assert!(test_no_straight_rewrite(
            "(>= (var a nil) (var b nil))",
            "true",
            &[]
        ));
        assert!(test_no_straight_rewrite("(>= 1 (var b nil))", "false", &[]));
        assert!(test_no_straight_rewrite("(>= 1 (var b nil))", "true", &[]));
    }

    #[test]
    fn vars_lt() {
        assert!(test_no_straight_rewrite(
            "(< (var a nil) (var b nil))",
            "false",
            &[]
        ));
        assert!(test_no_straight_rewrite("(< (var a nil) (var b nil))", "true", &[]));
        assert!(test_no_straight_rewrite("(< 1 (var b nil))", "false", &[]));
        assert!(test_no_straight_rewrite("(< 1 (var b nil))", "true", &[]));
    }

    #[test]
    fn vars_lte() {
        assert!(test_no_straight_rewrite(
            "(<= (var a nil) (var b nil))",
            "false",
            &[]
        ));
        assert!(test_no_straight_rewrite(
            "(<= (var a nil) (var b nil))",
            "true",
            &[]
        ));
        assert!(test_no_straight_rewrite("(<= 1 (var b nil))", "false", &[]));
        assert!(test_no_straight_rewrite("(<= 1 (var b nil))", "true", &[]));
    }

    // Helper functions
    fn ensure_const_prop(start: &str, expected: Peg) -> bool {
        let start_expr = start.parse().unwrap();
        // let mut expected_expr: RecExpr<Peg> = RecExpr::default();
        //expected_expr.add(Peg::Num(expected));
        let mut egraph = EGraph::default();
        let id = egraph.add_expr(&start_expr);
        let rules: Box<RewriteSystem> = crate::rewrites::rw_rules();
        let runner = Runner::default().with_egraph(egraph).run(rules.iter());
        runner.egraph[id].data.constant == Some(expected)
    }

    fn ensure_is_error(start: &str) -> bool {
        let start_expr = start.parse().unwrap();
        // let mut expected_expr: RecExpr<Peg> = RecExpr::default();
        //expected_expr.add(Peg::Num(expected));
        let mut egraph = EGraph::default();
        let id = egraph.add_expr(&start_expr);
        let rules: Box<RewriteSystem> = crate::rewrites::rw_rules();
        let runner = Runner::default().with_egraph(egraph).run(rules.iter());
        match runner.egraph[id].data.constant {
            Some(Peg::Error) => true,
            _ => false,
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
        assert!(test_straight_rewrite(
            "(== 1  (phi (var c nil) 1 2))",
            "(phi (var c nil) true false)"
        ));
    }

    #[test]
    fn eq_propagation_6() {
        assert!(!test_straight_rewrite("(== 1  (phi (var c nil) 1 2))", "true"));
    }

    #[test]
    fn eq_propagation_7() {
        assert!(test_straight_rewrite(
            "(== 1  (phi (var c nil) 1 2))",
            "(var c nil)"
        ));
    }

    #[test]
    fn eq_propagation_8() {
        assert!(test_straight_rewrite(
            "(== 1  (phi (var c nil) 2 1))",
            "(! (var c nil))"
        ));
    }

    #[test]
    fn eq_propagation_9() {
        assert!(test_straight_rewrite(
            "(== 2  (phi (var c nil) 2 1))",
            "(var c nil)"
        ));
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
        assert!(test_straight_rewrite(
            "(!= 1  (phi (var c nil) 1 2))",
            "(phi (var c nil) false true)"
        ));
        assert!(test_straight_rewrite(
            "(!= 1  (phi (var c nil) 1 2))",
            "(! (var c nil))"
        ));
    }

    #[test]
    fn neq_propagation_6() {
        assert!(!test_straight_rewrite("(!= 1  (phi (var c nil) 1 2))", "true"));
    }

    #[test]
    fn neq_propagation_7() {
        assert!(!test_straight_rewrite("(!= 1  (phi (var c nil) 1 2))", "false"));
    }

    #[test]
    fn neq_propagation_8() {
        assert!(test_no_straight_rewrite(
            "(!= 1  (phi (var c nil) 3 2))",
            "false",
            &[]
        ));
    }

    #[test]
    fn neq_propagation_9() {
        assert!(test_straight_rewrite("(!= 1  (phi (var c nil) 3 2))", "true"));
    }
}

#[cfg(test)]
mod refine_equalities {
    use super::*;

    #[test]
    #[ignore]
    fn eq_refine_const_pass() {
        let start = "(phi (== (var a nil) 5) (var a nil) 5)";
        let end = "5";
        assert!(test_straight_rewrite(start, end));
    }

    #[test]
    #[ignore]
    fn eq_refine_var_trigger() {
        assert!(test_straight_rewrite(
            "(phi (== (var a nil) (var b nil)) (var a nil) (var b nil))",
            "(var b nil)"
        ));
    }
}

#[cfg(test)]
mod distribute_over_phi {
    use super::*;

    #[test]
    fn push_plus_over_phi_1() {
        assert!(test_straight_rewrite(
            "(+ (phi (var a nil) 3 4) 1)",
            "(phi (var a nil) 4 5)"
        ));
    }

    #[test]
    fn push_plus_over_phi_2() {
        assert!(test_straight_rewrite(
            "(+ (phi (var a nil) (phi (var b nil) 1 2) 3) 1)",
            "(phi (var a nil) (phi (var b nil) 2 3) 4)"
        ));
    }

    #[test]
    fn push_ge_over_phi_a() {
        assert!(test_straight_rewrite("(>= 1 (phi (var a nil) 1 1))", "true"));
    }

    #[test]
    fn push_ge_over_phi_b() {
        assert!(test_straight_rewrite("(>= 2 (phi (var a nil) 0 1))", "true"));
    }

    #[test]
    fn push_ge_over_phi_1() {
        assert!(test_straight_rewrite(
            "(>= (phi (var a nil) (phi (var b nil) 0 2) 3) 4)",
            "false"
        ));
    }

    #[test]
    fn push_ge_over_phi_2() {
        assert!(test_straight_rewrite(
            "(>= (phi (var a nil) (phi (var b nil) 1 2) 3) 1)",
            "true"
        ));
    }

    #[test]
    fn push_ge_over_phi_3() {
        assert!(test_straight_rewrite(
            "(>=  2 (phi (var a nil) (phi (var b nil) 0 2) 3) )",
            "(var a nil)"
        ));
    }

    #[test]
    fn push_ge_over_phi_4() {
        assert!(test_straight_rewrite(
            "(>= 3 (phi (var a nil) (phi (var b nil) 1 2) 3))",
            "true"
        ));
    }

    #[test]
    fn push_le_over_phi_1() {
        assert!(test_no_straight_rewrite(
            "(<= 1 (phi (var a nil) (phi (var b nil) 0 2) 3))",
            "false",
            &[]
        ));
    }

    #[test]
    fn push_le_over_phi_2() {
        assert!(test_straight_rewrite(
            "(<= 1 (phi (var a nil) (phi (var b nil) 1 2) 3))",
            "true"
        ));
    }

    #[test]
    fn push_le_over_phi_3() {
        assert!(test_straight_rewrite(
            "(<= (phi (var a nil) (phi (var b nil) 0 2) 3) 2)",
            "(var a nil)"
        ));
    }

    #[test]
    fn push_le_over_phi_4() {
        assert!(test_straight_rewrite(
            "(<= (phi (var a nil) (phi (var b nil) 1 2) 3) 3)",
            "true"
        ));
    }

    #[test]
    fn push_gt_over_phi_1() {
        assert!(test_no_straight_rewrite(
            "(> (phi (var a nil) (phi (var b nil) 1 2) 3) 1)",
            "false",
            &[]
        ));
    }

    #[test]
    fn push_gt_over_phi_2() {
        assert!(test_straight_rewrite(
            "(> (phi (var a nil) (phi (var b nil) 1 2) 3) 0)",
            "true"
        ));
    }

    #[test]
    fn push_gt_over_phi_3() {
        assert!(test_straight_rewrite(
            "(>  3 (phi (var a nil) (phi (var b nil) 0 1) 2) )",
            "true"
        ));
    }

    #[test]
    fn push_gt_over_phi_4() {
        assert!(test_straight_rewrite(
            "(> 4 (phi (var a nil) (phi (var b nil) 1 2) 3))",
            "true"
        ));
    }

    #[test]
    fn push_gt_over_phi_5() {
        assert!(test_no_straight_rewrite(
            "(> 4 (phi (var a nil) (phi (var b nil) 1 2) 4))",
            "true",
            &[]
        ));
    }

    #[test]
    fn push_lt_over_phi_1() {
        assert!(test_straight_rewrite(
            "(< 0 (phi (var a nil) (phi (var b nil) 1 2) 3))",
            "true"
        ));
    }

    #[test]
    fn push_lt_over_phi_2() {
        assert!(test_straight_rewrite(
            "(< 1 (phi (var a nil) (phi (var b nil) 2 3) 4))",
            "true"
        ));
    }

    #[test]
    fn push_lt_over_phi_3() {
        assert!(test_straight_rewrite(
            "(< (phi (var a nil) (phi (var b nil) 0 2) 3) 0)",
            "false"
        ));
    }

    #[test]
    fn push_lt_over_phi_4() {
        assert!(test_straight_rewrite(
            "(< (phi (var a nil) (phi (var b nil) 1 2) 3) 4)",
            "true"
        ));
    }
}

#[cfg(test)]
mod misc {
    use super::*;

    #[test]
    fn test_max_1() {
        assert!(test_no_straight_rewrite("(phi (! (phi (> (var a nil) (var b nil)) true false)) (var b nil) (phi (> (var a nil) (var b nil)) (var a nil) -2147483648))", "(var b nil)", &[]))
    }

    #[test]
    fn test_max_2() {
        assert!(test_no_straight_rewrite("(phi (! (phi (!= (var a nil) (var b nil)) true false)) (var b nil) (phi (!= (var a nil) (var b nil)) (var a nil) -2147483648))", "(var b nil)", &[]))
    }

    #[test]
    fn test_max_3() {
        assert!(test_no_straight_rewrite("(phi (! (phi (>= (var a nil) (var b nil)) true false)) (var b nil) (phi (>= (var a nil) (var b nil)) (var a nil) -2147483648))", "(var b nil)", &[]))
    }

    #[test]
    fn test_max_4() {
        assert!(test_straight_rewrite(
            "(phi (! (phi false true false)) (var b nil) (phi false (var a nil) -2147483648))",
            "(var b nil)"
        ));
    }

    #[test]
    fn ensure_no_rewrite_1() {
        // The following are all just sanity checks. These all pass
        assert!(test_no_straight_rewrite(
            "(phi (>= (var a nil) (var b nil)) true false)",
            "true",
            &[]
        ));
        assert!(test_no_straight_rewrite(
            "(phi (>= (var a nil) (var b nil)) true false)",
            "false",
            &[]
        ));
        assert!(test_no_straight_rewrite(
            "(phi (<= (var a nil) (var b nil)) true false)",
            "true",
            &[]
        ));
        assert!(test_no_straight_rewrite(
            "(phi (<= (var a nil) (var b nil)) true false)",
            "false",
            &[]
        ));
        assert!(test_no_straight_rewrite(
            "(phi (> a b) true false)",
            "true",
            &[]
        ));
        assert!(test_no_straight_rewrite(
            "(phi (< a b) true false)",
            "true",
            &[]
        ));
        assert!(test_no_straight_rewrite(
            "(phi (> (var a nil) (var b nil)) true false)",
            "true",
            &[]
        ));
        assert!(test_no_straight_rewrite(
            "(phi (< (var a nil) (var b nil)) true false)",
            "true",
            &[]
        ));
        assert!(test_no_straight_rewrite(
            "(phi (> a b) true false)",
            "false",
            &[]
        ));
        assert!(test_no_straight_rewrite(
            "(phi (< a b) true false)",
            "false",
            &[]
        ));
    }

    #[test]
    fn ensure_no_rewrite_2() {
        assert!(test_no_straight_rewrite(
            "(> (var a nil) (var b nil))",
            "false",
            &["(! (phi (> (var a nil) (var b nil)) true false))"]
        ))
    }

    #[test]
    fn ensure_no_rewrite_3() {
        assert!(test_no_straight_rewrite(
            "false",
            "true",
            &["(! (phi (> (var a nil) (var b nil)) true false))"]
        ));
    }

    #[test]
    #[ignore]
    fn eq_distr_var_1() {
        // Ignored until equality distribution is restored
        assert!(test_straight_rewrite(
            "(eq-distr (var a nil) (var b nil) (var c nil))",
            "(var c nil)"
        ));
    }

    #[test]
    #[ignore]
    fn eq_distr_eq_1() {
        // Ignored until equality distribution is restored
        assert!(test_straight_rewrite(
            "(eq-distr (var a nil) (var b nil) (== (var a nil) (var b nil)))",
            "true"
        ));
    }

    #[test]
    fn add_ac_overflow() {
        // The following are regression tests to prevent against overflow bugs. More details here:
        // https://bkushigian.github.io/2020/08/10/debugging-rewrites-1.html
        assert!(rewrites_do_not_panic(&[
            "(+ (phi C 1 0) 2)",
            "(phi true 0 -2147483648)"
        ]));
        assert!(rewrites_do_not_panic(&[
            "(+ (phi C 1 1) 2)",
            "(phi true 0 -2147483648)"
        ]));
        assert!(rewrites_do_not_panic(&[
            "(+ (phi C 1 1) 0)",
            "(phi true 0 -2147483648)"
        ]));
        assert!(rewrites_do_not_panic(&[
            "(+ 1 0)",
            "(phi true 0 -2147483648)"
        ]));
        assert!(rewrites_do_not_panic(&[
            "(+ (phi C 0 0) 0)",
            "(phi true 0 -2147483648)"
        ]));
        assert!(rewrites_do_not_panic(&[
            "(+ (phi C 1 0) 1)",
            "(phi true 0 -76650000)"
        ]));
        assert!(rewrites_do_not_panic(&[
            "(+ (phi C 1 0) 1)",
            "(phi true 0 -76700000)"
        ]));
        assert!(rewrites_do_not_panic(&[
            "(+ (phi C 1 0) 1)",
            "(phi true 0 -76695844)"
        ]));
        assert!(rewrites_do_not_panic(&[
            "(+ (phi C 1 0) 1)",
            "(phi true 0 -76695845)"
        ]));
        assert!(rewrites_do_not_panic(&[
            "(+ (phi C 0 0) 1)",
            "(phi true 1 -2147483648)"
        ]));
        assert!(rewrites_do_not_panic(&[
            "(+ (phi C 1 0) 1)",
            "(phi true 1 -2147483648)"
        ]));
        assert!(rewrites_do_not_panic(&[
            "(+ (phi true 0 -2147483647) 0)",
            "(phi true 0 -2147483647)"
        ]));
    }
}

#[cfg(test)]
mod heap {
    use super::*;
    #[test]
    fn rd_wr() {
        assert!(test_straight_rewrite(
            "(rd (path (var a nil) (derefs .a.b)) (wr (path (var a nil) (derefs .a.b)) 3 (heap 0 unit)))",
            "3"
        ));
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

            "(return-node (rd (path (var this nil) (derefs x)) (wr (path (var this nil) (derefs x)) (- (var a nil) (var a nil)) (heap 0 unit))) (wr (path (var this nil) (derefs x)) (- (var a nil) (var a nil)) (heap 0 unit)))",
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

            "(return-node 0 (wr (path (var this nil) (derefs x)) (- (var a nil) (var a nil)) (heap 0 unit)))"))
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
    (rd (path (var this nil) (derefs x))
        (wr (path (var this nil) (derefs x))
            (phi (> (var a nil) (var b nil)) (var a nil) (var b nil))
            (heap 0 unit)))
    (wr (path (var this nil) (derefs x))
        (phi (> (var a nil) (var b nil)) (var a nil) (var b nil))
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
    (rd (path (var this nil) (derefs x))
        (wr (path (var this nil) (derefs x))
            (phi (>= (var a nil) (var b nil)) (var a nil) (var b nil))
            (heap 0 unit)))
    (wr (path (var this nil) (derefs x))
        (phi (>= (var a nil) (var b nil)) (var a nil) (var b nil))
        (heap 0 unit)))"
        ))
    }
}

#[cfg(test)]
mod serialization {
    use super::*;
    #[test]
    fn field_write_gt() {
        ensure_serialized_subject_meets_specs(
            "../tests/field-write.xml",
            "../tests/field-write.gt",
            1,
        );
    }

    #[test]
    fn field_write_deduplication() {
        ensure_no_duplicates_in_serialized("../tests/field-write.xml");
    }

    #[test]
    fn field_access_gt() {
        ensure_serialized_subject_meets_specs(
            "../tests/field-access.xml",
            "../tests/field-access.gt",
            1,
        );
    }

    #[test]
    fn field_access_deduplication() {
        ensure_no_duplicates_in_serialized("../tests/field-access.xml");
    }

    #[test]
    fn method_invocation_gt() {
        ensure_serialized_subject_meets_specs(
            "../tests/method-invocation.xml",
            "../tests/method-invocation.gt",
            1,
        );
    }

    #[test]
    fn method_invocation_deduplication() {
        ensure_no_duplicates_in_serialized("../tests/method-invocation.xml");
    }

    #[test]
    fn loop_deduplication() {
        ensure_no_duplicates_in_serialized("../tests/loops.xml");
    }
}

#[cfg(test)]
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
    let runner = Runner::default().with_egraph(eg).run(rules.iter());
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
            _ => false,
        },
        _ => false,
    };

    if debug {
        println!("debug=true");
    }

    let mut trial: u32 = 0;

    if exprs_collide(start, end, other) {
        if run_dd {
            let start_expr = start.parse().unwrap();
            let end_expr = end.parse().unwrap();
            let oracle = move |config: &[&Rewrite<Peg, PegAnalysis>]| -> bool {
                if debug {
                    println!(
                        "Trial {}: running oracle on len {} config",
                        trial,
                        config.len()
                    );
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
                if debug && !not_equiv {
                    println!("    Found new minimal config");
                    for (i, rule) in config.iter().enumerate() {
                        println!("    ({}) {}", i + 1, rule.name());
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
                println!("({}) {}", i + 1, rule.name());
            }
            if debug {
                let seconds = dd_start_time.elapsed().as_secs();
                println!(
                    "Minimized buggy input from {} rules to {} rules in {} seconds",
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
fn rewrites_do_not_panic(exprs: &[&str]) -> bool {
    let mut egraph: EGraph<Peg, PegAnalysis> = EGraph::default();
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
    min_score: u32,
) {
    let subjects = crate::subjects::run_on_subjects_file(
        subjects_file,
        &RunConfig::default(),
        &mut GlobalData::default(),
    )
    .unwrap();
    let mut score = 0;
    for subject in subjects.subjects {
        let eq_rel = EqRel::from(&subject.analysis_result);
        let gt_rel: EqRel<u32> = read_to_string(ground_truth_file)
            .map(|cs| EqRel::from(cs.as_str()))
            .unwrap();

        assert!(
            eq_rel.is_refinement_of(&gt_rel),
            "{:?} does not refine {:?}",
            eq_rel,
            gt_rel
        );
        score += subject.analysis_result.score;
    }
    assert!(
        score >= min_score,
        "score = {} < min_score = {}",
        score,
        min_score
    );
}

/// Read in a serialized file, parse it into a subjects, and add it to an
/// EGraph. Then, check that the size of the EGraph is correct (i.e., that there
/// were no duplications of syntactically identical pegs from serialization).
#[allow(dead_code)]
fn ensure_no_duplicates_in_serialized(subjects_file: &str) {
    let mut subjects: crate::subjects::Subjects =
        crate::subjects::Subject::from_file(subjects_file.to_string()).unwrap();
    let rec_expr = subjects.compute_rec_expr().unwrap();
    let rec_expr_ref = rec_expr.as_ref();
    let mut egraph = EGraph::<Peg, ()>::default();

    for i in 0..rec_expr_ref.len() {
        let new_id = egraph.add(rec_expr_ref[i].clone());

        let egg_size = egraph.total_size();
        let v = rec_expr_ref[0..(i + 1)].to_vec();
        let re = RecExpr::from(v);
        assert!(
            (i + 1) == egg_size,
            "rec_expr_size: {} != egraph size: {}\nre: `{}`",
            i + 1,
            egg_size,
            re.pretty(80)
        );
        assert!(
            usize::from(new_id) == i,
            "id: {}, i: {}",
            usize::from(new_id),
            i
        );
    }

    let mut egraph = EGraph::<Peg, ()>::default();
    egraph.add_expr(&rec_expr);
    let egg_size = egraph.total_size();
    let ser_size = rec_expr.as_ref().len();
    assert!(
        ser_size == egg_size,
        "serialized size: {} != egraph size: {}",
        ser_size,
        egg_size
    );
}

#[allow(dead_code)]
fn try_to_parse(expr: &str) -> bool {
    let start_expr:Result<RecExpr<Peg>, String> = expr.parse();
    match start_expr {
        Ok(_) => {
            return true;
        }
        Err(s) => {
            println!("Error parsing {}: Error: {}", expr, s);
            return false;
        }
    }
}