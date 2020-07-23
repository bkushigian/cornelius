use egg::{rewrite as rw, Rewrite};
use crate::peg::*;

pub type RewriteSystem = [Rewrite<Peg, VarAnalysis>];


#[allow(unused_parens)]
pub fn rw_rules() -> Box<RewriteSystem> {
  let rules = [
    // Arithmetic
    rw!("commute-add";   "(+ ?a ?b)"         => "(+ ?b ?a)"),
    rw!("commute-mul";   "(* ?a ?b)"         => "(* ?b ?a)"),
    rw!("associate-add"; "(+ ?a (+ ?b ?c))"  => "(+ (+ ?a ?b) ?c)"),
    rw!("associate-mul"; "(* ?a (* ?b ?c))"  => "(* (* ?a ?b) ?c)"),
    rw!("add-ident";     "(+ ?a 0)"          => "?a"),
    rw!("mul-bot";       "(* ?a 0)"          => "0"),
    rw!("mul-ident";     "(* ?a 1)"          => "?a"),
    rw!("neg-zero";      "(--- 0)"           => "0"),
    rw!("add-inv";       "(+ ?a (--- ?a))"   => "0"),
    rw!("sub-to-add";    "(- ?a ?b)"         => "(+ ?a (--- ?b))"),

    // Logical
    rw!("commute-and";   "(&& ?a ?b)"         => "(&& ?b ?a)"),
    rw!("commute-or";    "(|| ?a ?b)"         => "(|| ?b ?a)"),
    rw!("associate-and"; "(&& ?a (&& ?b ?c))" =>"(&& (&& ?a ?b) ?c)" ),
    rw!("associate-or";  "(|| ?a (|| ?b ?c))" => "(|| (|| ?a ?b) ?c)"),
    rw!("and-ident";     "(&& ?a true)"  => "?a"),
    rw!("or-ident";      "(|| ?a false)" => "?a"),
    rw!("and-bot";       "(&& ?a false)" => "false"),
    rw!("or-bot";        "(|| ?a true)"  => "true"),
    rw!("not-inv";       "(! (! ?a))"         => "?a"),
    rw!("not-dist-and";  "(! (&& ?a ?b))"     => "(|| (! ?a) (! ?b))"),
    rw!("not-dist-or";   "(! (|| ?a ?b))"     => "(&& (! ?a) (! ?b))"),
    rw!("a-and-not-a";   "(&& ?a (! ?a))"     => "false"),
    rw!("a-or-not-a";    "(|| ?a (! ?a))"     => "true"),
    rw!("eq-comm";       "(== ?a ?b)"         => "(== ?b ?a)"),
    rw!("not-not";       "(! (! ?a))"         => "?a" ),
    rw!("not-eq-to-neq"; "(! (== ?a ?b))"     => "(!= ?a ?b)" ),
    rw!("neq-to-not-eq"; "(!= ?a ?b)"         => "(! (== ?a ?b))" ),
    rw!("phi-as-cond";   "(phi ?c true false)" => "?c" ),

    // Equality
    rw!("refl";    "(== ?a ?a)"   => "true"),


    // Equality Shuffling
    rw!("eq-shuff1";    "(== (+ ?a ?b) ?c)"   => "(== ?a (- ?c ?b))"),
    rw!("eq-shuff2";    "(== ?a (- ?c ?b))"   => "(== (+ ?a ?b) ?c)"),

    // Bitwise
    rw!("commute-bin-and";   "(& ?a ?b)"    => "(& ?b ?a)"),
    rw!("commute-bin-or";    "(| ?a ?b)"    => "(| ?b ?a)"),
    rw!("associate-bin-and"; "(& ?a (& ?b ?c))"   => "(& (& ?a ?b) ?c)" ),
    rw!("associate-bin-or";  "(| ?a (| ?b ?c))"   => "(| (| ?a ?b) ?c)"),
    rw!("bin-and-ident";     "(& ?a -1)"  => "?a"),
    rw!("bin-or-ident";      "(| ?a 0)"   => "?a"),
    rw!("bin-and-bot";       "(& ?a 0)"   => "0"),
    rw!("bin-or-bot";        "(| ?a -1)"  => "-1"),

    // Ops Distr over Phi
    rw!("plus-over-phi";      "(+ (phi ?c ?t ?e) ?rhs)"  => "(phi ?c (+ ?t ?rhs) (+ ?e ?rhs))"),
    rw!("gt-left-over-phi";   "(> ?a (phi ?c ?t ?e))"    => "(phi ?c (> ?a ?t) (> ?a ?e))")    ,
    rw!("ge-left-over-phi";   "(>= ?a (phi ?c ?t ?e))"   => "(phi ?c (>= ?a ?t) (>= ?a ?e))")  ,
    rw!("lt-left-over-phi";   "(< ?a (phi ?c ?t ?e))"    => "(phi ?c (< ?a ?t) (< ?a ?e))")    ,
    rw!("le-left-over-phi";   "(<= ?a (phi ?c ?t ?e))"   => "(phi ?c (<= ?a ?t) (<= ?a ?e))")  ,
    rw!("gt-right-over-phi";  "(>  (phi ?c ?t ?e) ?a)"   => "(phi ?c (>  ?t ?a) (>  ?e ?a))")  ,
    rw!("ge-right-over-phi";  "(>= (phi ?c ?t ?e) ?a)"   => "(phi ?c (>= ?t ?a) (>= ?e ?a))")  ,
    rw!("lt-right-over-phi";  "(<  (phi ?c ?t ?e) ?a)"   => "(phi ?c (<  ?t ?a) (<  ?e ?a))")  ,
    rw!("le-right-over-phi";  "(<= (phi ?c ?t ?e) ?a)"   => "(phi ?c (<= ?t ?a) (<= ?e ?a))")  ,
    rw!("and-right-over-phi"; "(&& (phi ?c ?t ?e) ?a)"   => "(phi ?c (&& ?t ?a) (&& ?e ?a))")  ,
    rw!("or-right-over-phi";  "(|| (phi ?c ?t ?e) ?a)"   => "(phi ?c (|| ?t ?a) (|| ?e ?a))")  ,
    rw!("not-over-phi";       "(! (phi ?c ?t ?e))"       => "(phi ?c (! ?t) (! ?e))")          ,
    rw!("eq-over-phi";        "(== ?a (phi ?c ?t ?e))"   => "(phi ?c (== ?a ?t) (== ?a ?e))")  ,

    rw!("inv-plus-over-phi";      "(phi ?c (+ ?t ?rhs) (+ ?e ?rhs))"  => "(+ (phi ?c ?t ?e) ?rhs)" ),
    rw!("inv-gt-left-over-phi";   "(phi ?c (> ?a ?t) (> ?a ?e))"      => "(> ?a (phi ?c ?t ?e))"   ),
    rw!("inv-ge-left-over-phi";   "(phi ?c (>= ?a ?t) (>= ?a ?e))"    => "(>= ?a (phi ?c ?t ?e))"  ),
    rw!("inv-lt-left-over-phi";   "(phi ?c (< ?a ?t) (< ?a ?e))"      => "(< ?a (phi ?c ?t ?e))"   ),
    rw!("inv-le-left-over-phi";   "(phi ?c (<= ?a ?t) (<= ?a ?e))"    => "(<= ?a (phi ?c ?t ?e))"  ),
    rw!("inv-gt-right-over-phi";  "(phi ?c (>  ?t ?a) (>  ?e ?a))"    => "(>  (phi ?c ?t ?e) ?a)"  ),
    rw!("inv-ge-right-over-phi";  "(phi ?c (>= ?t ?a) (>= ?e ?a))"    => "(>= (phi ?c ?t ?e) ?a)"  ),
    rw!("inv-lt-right-over-phi";  "(phi ?c (<  ?t ?a) (<  ?e ?a))"    => "(<  (phi ?c ?t ?e) ?a)"  ),
    rw!("inv-le-right-over-phi";  "(phi ?c (<= ?t ?a) (<= ?e ?a))"    => "(<= (phi ?c ?t ?e) ?a)"  ),
    rw!("inv-and-right-over-phi"; "(phi ?c (&& ?t ?a) (&& ?e ?a))"    => "(&& (phi ?c ?t ?e) ?a)"  ),
    rw!("inv-or-right-over-phi";  "(phi ?c (|| ?t ?a) (|| ?e ?a))"    => "(|| (phi ?c ?t ?e) ?a)"  ),
    rw!("inv-not-over-phi";       "(phi ?c (! ?t) (! ?e))"            => "(! (phi ?c ?t ?e))"      ),
    rw!("inv-eq-over-phi";        "(phi ?c (== ?a ?t) (== ?a ?e))"    => "(== ?a (phi ?c ?t ?e))"  ),

    // Ordering
    rw!("lt-comp";      "(< ?a ?b)"     => "(! (>= ?a ?b))"),
    rw!("gt-comp";      "(> ?a ?b)"     => "(! (<= ?a ?b))"),
    rw!("lte-comp";     "(<= ?a ?b)"    => "(! (> ?a ?b))"),
    rw!("gte-comp";     "(>= ?a ?b)"    => "(! (< ?a ?b))"),
    rw!("gte-split";    "(>= ?a ?b)"    => "(|| (> ?a ?b) (== ?a ?b))"),
    rw!("lte-split";    "(<= ?a ?b)"    => "(|| (< ?a ?b) (== ?a ?b))"),

    // Branching
    rw!("if-true";     "(phi true ?x ?y)"        => "?x"),
    rw!("switch-branching";     "(phi ?c ?x ?y)" => "(phi (! ?c) ?y ?x)"),
    rw!("if-false";    "(phi false ?x ?y)"       => "?y"),
    rw!("nesting-and"; "(phi (&& ?a ?b) ?t ?e)"  => "(phi ?a (phi ?b ?t ?e) ?e)"),
    rw!("same-branch"; "(phi ?c ?t ?t)"          => "?t"),

    // Equality Refinement
    // swap all a's for b's in t
    //rw!("equality-refinement"; "(phi (== (var ?a) ?b) ?t ?e)" =>
    //    "(phi (== (var ?a) ?b) (swap (var ?a) ?b ?t) ?e)"),

    //// Swapping
    //rw!("swap-match";        "(swap (var ?a) ?b (var ?a))"         => "?b"),
    //rw!("swap-var-pass-through"; "(swap (var ?a) ?b ?c)"           => "?c" if is_not_same_var("?a", "?c")),
    //rw!("swap-const-pass-through"; "(swap (var ?a) ?b ?c)"         => "?c" if is_const("?c")),
    //rw!("swap-or";   "(swap (var ?a) ?b (|| ?l ?r))"  => "(|| (swap (var ?a) ?b ?l) (swap (var ?a) ?b ?r))"),
    //rw!("swap-and";  "(swap (var ?a) ?b (&& ?l ?r))"  => "(&& (swap (var ?a) ?b ?l) (swap (var ?a) ?b ?r))"),
    //rw!("swap-plus"; "(swap (var ?a) ?b (+ ?l ?r))"   => "(+  (swap (var ?a) ?b ?l) (swap (var ?a) ?b ?r))"),
    //rw!("swap-sub";  "(swap (var ?a) ?b (- ?l ?r))"   => "(-  (swap (var ?a) ?b ?l) (swap (var ?a) ?b ?r))"),
    //rw!("swap-mul";  "(swap (var ?a) ?b (* ?l ?r))"   => "(*  (swap (var ?a) ?b ?l) (swap (var ?a) ?b ?r))"),
    //rw!("swap-div";  "(swap (var ?a) ?b (/ ?l ?r))"   => "(/  (swap (var ?a) ?b ?l) (swap (var ?a) ?b ?r))"),
    //rw!("swap-lt";   "(swap (var ?a) ?b (< ?l ?r))"   => "(<  (swap (var ?a) ?b ?l) (swap (var ?a) ?b ?r))"),
    //rw!("swap-gt";   "(swap (var ?a) ?b (> ?l ?r))"   => "(>  (swap (var ?a) ?b ?l) (swap (var ?a) ?b ?r))"),
    //rw!("swap-le";   "(swap (var ?a) ?b (<= ?l ?r))"  => "(<= (swap (var ?a) ?b ?l) (swap (var ?a) ?b ?r))"),
    //rw!("swap-ge";   "(swap (var ?a) ?b (>= ?l ?r))"  => "(>= (swap (var ?a) ?b ?l) (swap (var ?a) ?b ?r))"),
    //rw!("swap-neq";  "(swap (var ?a) ?b (!= ?l ?r))"  => "(!= (swap (var ?a) ?b ?l) (swap (var ?a) ?b ?r))"),
    //rw!("swap-eq";   "(swap (var ?a) ?b (== ?l ?r))"  => "(== (swap (var ?a) ?b ?l) (swap (var ?a) ?b ?r))"),


    //rw!("condition-distribution"; "(phi ?c ?t ?e)"
    //    => "(phi ?c (cond-distr ?c ?t) (cond-distr ?c e))"),

    //rw!("cond-distr-phi";  "(cond-distr ?c (phi ?d ?t ?e))"
    //    => "(phi (cond-distr ?c ?d) (cond-distr ?c ?t) (cond-distr ?c ?e))"),
    //rw!("cond-distr-or";   "(cond-distr ?c (|| ?l ?r))"  => "(|| (cond-distr ?c ?l) (cond-distr ?c ?r))"),
    //rw!("cond-distr-and";  "(cond-distr ?c (&& ?l ?r))"  => "(&& (cond-distr ?c ?l) (cond-distr ?c ?r))"),
    //rw!("cond-distr-plus"; "(cond-distr ?c (+ ?l ?r))"   => "(+  (cond-distr ?c ?l) (cond-distr ?c ?r))"),
    //rw!("cond-distr-sub";  "(cond-distr ?c (- ?l ?r))"   => "(-  (cond-distr ?c ?l) (cond-distr ?c ?r))"),
    //rw!("cond-distr-mul";  "(cond-distr ?c (* ?l ?r))"   => "(*  (cond-distr ?c ?l) (cond-distr ?c ?r))"),
    //rw!("cond-distr-div";  "(cond-distr ?c (/ ?l ?r))"   => "(/  (cond-distr ?c ?l) (cond-distr ?c ?r))"),
    //rw!("cond-distr-lt";   "(cond-distr ?c (< ?l ?r))"   => "(<  (cond-distr ?c ?l) (cond-distr ?c ?r))"),
    //rw!("cond-distr-gt";   "(cond-distr ?c (> ?l ?r))"   => "(>  (cond-distr ?c ?l) (cond-distr ?c ?r))"),
    //rw!("cond-distr-le";   "(cond-distr ?c (<= ?l ?r))"  => "(<= (cond-distr ?c ?l) (cond-distr ?c ?r))"),
    //rw!("cond-distr-ge";   "(cond-distr ?c (>= ?l ?r))"  => "(>= (cond-distr ?c ?l) (cond-distr ?c ?r))"),
    //rw!("cond-distr-not";  "(cond-distr ?c (! ?x))"      => "(!  (cond-distr ?c ?x))"),
    //rw!("cond-distr-eq";   "(cond-distr ?c ?c)"          => "true"),
    //rw!("cond-distr-neq";  "(cond-distr ?c (! ?c))"      => "false"),
    //rw!("cond-distr-var";   "(cond-distr ?c (var ?v))"    => "(var ?v)"),
    //rw!("cond-distr-const"; "(cond-distr ?c ?n)"    => "?n" if is_const("?n")),
  ];
  Box::new(rules)
}
