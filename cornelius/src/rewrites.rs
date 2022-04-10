use crate::peg::*;
use egg::{rewrite as rw, Rewrite};

pub type RewriteSystem = [Rewrite<Peg, PegAnalysis>];

#[allow(unused_parens)]
pub fn rw_rules() -> Box<RewriteSystem> {
    let rules = [
        // Arithmetic
        rw!("commute-add";   "(+ ?a ?b)"         => "(+ ?b ?a)"),
        rw!("commute-mul";   "(* ?a ?b)"         => "(* ?b ?a)"),
        rw!("associate-add"; "(+ ?a (+ ?b ?c))"  => "(+ (+ ?a ?b) ?c)"),
        rw!("associate-mul"; "(* ?a (* ?b ?c))"  => "(* (* ?a ?b) ?c)"),
        rw!("add-ident";     "(+ ?a 0)"          => "?a" if is_not_const("?a")),
        rw!("mul-bot";       "(* ?a 0)"          => "0" if is_not_const("?a")),
        rw!("mul-bot";       "(* ?a 0l)"         => "0l" if is_not_const("?a")),
        rw!("mul-ident";     "(* ?a 1)"          => "?a" if is_not_const("?a")),
        rw!("neg-zero";      "(--- 0)"           => "0"),
        rw!("add-inv";       "(+ ?a (--- ?a))"   => "0" if is_not_const("?a")),
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
        rw!("gt-left-over-phi";   "(> ?a (phi ?c ?t ?e))"    => "(phi ?c (> ?a ?t) (> ?a ?e))"),
        rw!("ge-left-over-phi";   "(>= ?a (phi ?c ?t ?e))"   => "(phi ?c (>= ?a ?t) (>= ?a ?e))"),
        rw!("lt-left-over-phi";   "(< ?a (phi ?c ?t ?e))"    => "(phi ?c (< ?a ?t) (< ?a ?e))"),
        rw!("le-left-over-phi";   "(<= ?a (phi ?c ?t ?e))"   => "(phi ?c (<= ?a ?t) (<= ?a ?e))"),
        rw!("gt-right-over-phi";  "(>  (phi ?c ?t ?e) ?a)"   => "(phi ?c (>  ?t ?a) (>  ?e ?a))"),
        rw!("ge-right-over-phi";  "(>= (phi ?c ?t ?e) ?a)"   => "(phi ?c (>= ?t ?a) (>= ?e ?a))"),
        rw!("lt-right-over-phi";  "(<  (phi ?c ?t ?e) ?a)"   => "(phi ?c (<  ?t ?a) (<  ?e ?a))"),
        rw!("le-right-over-phi";  "(<= (phi ?c ?t ?e) ?a)"   => "(phi ?c (<= ?t ?a) (<= ?e ?a))"),
        rw!("and-right-over-phi"; "(&& (phi ?c ?t ?e) ?a)"   => "(phi ?c (&& ?t ?a) (&& ?e ?a))"),
        rw!("or-right-over-phi";  "(|| (phi ?c ?t ?e) ?a)"   => "(phi ?c (|| ?t ?a) (|| ?e ?a))"),
        rw!("not-over-phi";       "(! (phi ?c ?t ?e))"       => "(phi ?c (! ?t) (! ?e))"),
        rw!("eq-over-phi";        "(== ?a (phi ?c ?t ?e))"   => "(phi ?c (== ?a ?t) (== ?a ?e))"),
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

        /***                       Heapy Stuff                       ***/
        rw!("rd-wr"; "(rd ?path (wr ?path ?val ?heap))"  => "?val"),

        /***                       Type-Guided Rewrites                       ***/
        rw!("array-length"; "(< (rd (path (var ?v \"Array\") (derefs (length))) ?h) 0)" => "false"),
        rw!("collection-size"; "(< (invoke->peg (invoke ?hp (var ?v \"java.util.Collection\") (size) (actuals))) 0)" => "false"),
        rw!("string-length"; "(< (invoke->peg (invoke ?hp (var ?v \"java.lang.String\") (length) (actuals))) 0)" => "false"),
        rw!("string-index-of"; "(< (invoke->peg (invoke ?hp (var ?v \"java.lang.String\") (indexOf) (actuals ?a1))) -1)" => "false"),
        rw!("string-last-index-of"; "(< (invoke->peg (invoke ?hp (var ?v \"java.lang.String\") (lastIndexOf) (actuals ?a1))) -1)" => "false"),

        // NOTE: This following rewrite is optimistic and is not entirely sound
        // However, in practice this should be correct (and if it's not, there is bad code)
        rw!("string-starts-with"; 
            "(|| (invoke->peg (invoke ?hp1 (var ?v \"java.lang.String\") (startsWith) (actuals ?a)))
                 (invoke->peg (invoke ?hp2 (var ?v \"java.lang.String\") (startsWith) (actuals ?b))))" 
                => 
            "(!= (invoke->peg (invoke ?hp1 (var ?v \"java.lang.String\") (startsWith) (actuals ?a)))
                 (invoke->peg (invoke ?hp2 (var ?v \"java.lang.String\") (startsWith) (actuals ?b))))"),
                 
        rw!("string-starts-with-empty"; 
            "(invoke->peg (invoke ?hp (var ?v \"java.lang.String\") (startsWith) (actuals \"\")))" 
                => "true"),
        
    ];
    Box::new(rules)
}
