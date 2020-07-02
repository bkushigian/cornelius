use egg::{rewrite as rw, *};
use serde::Deserialize;
use serde_aux::prelude::*;
use serde_xml_rs::from_reader;

pub type RewriteSystem = [Rewrite<Peg, VarAnalysis>];
pub type Rules = Box<RewriteSystem>;

define_language! {
    pub enum Peg {
      Num(i32),
      Bool(bool),
      "+"    = Add([Id; 2]),
      "*"    = Mul([Id; 2]),
      "/"    = Div([Id; 2]),
      "-"    = Sub([Id; 2]),
      "---"    = Neg(Id),
      "&&"   = And([Id; 2]),
      "||"   = Or([Id; 2]),
      "&"    = BinAnd([Id; 2]),
      "|"    = BinOr([Id; 2]),
      "~"    = BinNeg(Id),
      ">>"   = SRShift([Id; 2]),
      ">>>"  = URShift([Id; 2]),
      "<<"   = LShift([Id; 2]),
      "!"    = Not(Id),
      "^"    = Xor([Id; 2]),
      "<"    = Lt([Id; 2]),
      "<="   = Lte([Id; 2]),
      ">"    = Gt([Id; 2]),
      ">="   = Gte([Id; 2]),
      "=="   = Equ([Id; 2]),
      "!="   = Neq([Id; 2]),
      "phi"  = Phi([Id; 3]),
      "swap" = Swap([Id; 3]),
      "eq-distr" = EqDistr([Id; 3]),
      "neq-distr" = NeqDistr([Id; 3]),
      "var"  = Var(Id),
      Symbol(String),
    }
}

pub type EGraph = egg::EGraph<Peg, VarAnalysis>;

#[derive(Default, PartialEq, Debug, Clone)]
pub struct AnalysisData {
  has_const: Option<Peg>,
  has_var: Option<Peg>
}

impl AnalysisData {
  pub fn or(self, a: AnalysisData) -> AnalysisData {
    AnalysisData {
      has_const: self.has_const.or(a.has_const),
      has_var: self.has_var.or(a.has_var)
    }
  }
}

#[derive(Default)]
pub struct VarAnalysis;


impl Analysis<Peg> for VarAnalysis {
    type Data = AnalysisData;
    fn merge(&self, to: &mut Self::Data, from: Self::Data) -> bool {
      merge_if_different(to, to.clone().or(from))
    }

    fn make(egraph: &EGraph, enode: &Peg) -> Self::Data {
        let _x = |i: &Id| egraph[*i].data.clone();
        match enode {
          Peg::Var(_) =>
            AnalysisData {
              has_var: Some(enode.clone()),
              has_const: None
            },
          Peg::Num(_) =>
            AnalysisData {
              has_const: Some(enode.clone()),
              has_var: None
            },
          _ => AnalysisData{ has_const: None, has_var: None },
        }
    }
}

fn is_const(v1: &'static str) -> impl Fn(&mut EGraph, Id, &Subst) -> bool {
    let v1: egg::Var = v1.parse().unwrap();
    move |egraph, _, subst| egraph[subst[v1]].data.has_const.is_some()
}

fn is_var(v1: &'static str) -> impl Fn(&mut EGraph, Id, &Subst) -> bool {
    let v1: egg::Var = v1.parse().unwrap();
    move |egraph, _, subst| egraph[subst[v1]].data.has_var.is_some()
}

fn is_not_same_var(v1: &'static str, v2: &'static str) -> impl Fn(&mut EGraph, Id, &Subst) -> bool {
    let v1: egg::Var = v1.parse().unwrap();
    let v2: egg::Var = v2.parse().unwrap();
    move |egraph: &mut EGraph, _, subst: &Subst| egraph.find(subst[v1]) != egraph.find(subst[v2])
}


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
    rw!("and-ident";     "(&& ?a true)"       => "?a"),
    rw!("or-ident";      "(|| ?a false)"      => "?a"),
    rw!("and-bot";       "(&& ?a false)"      => "false"),
    rw!("or-bot";        "(|| ?a true)"       => "true"),
    rw!("not-inv";       "(! (! ?a))"         => "?a"),
    rw!("not-dist-and";  "(! (&& ?a ?b))"     => "(|| (! ?a) (! ?b))"),
    rw!("not-dist-or";   "(! (|| ?a ?b))"     => "(&& (! ?a) (! ?b))"),
    rw!("a-and-not-a";   "(&& ?a (! ?a))"     => "false"),
    rw!("a-or-not-a";    "(|| ?a (! ?a))"     => "true"),
    rw!("eq-comm";       "(== ?a ?b)"         => "(== ?b ?a)"),
    rw!("not-not";       "(! (! ?a))"         => "?a" ),
    rw!("not-eq-to-neq"; "(! (== ?a ?b))"         => "(!= ?a ?b)" ),
    rw!("neq-to-not-eq"; "(!= ?a ?b)"         => "(! (== ?a ?b))" ),
    rw!("phi-as-cond";   "(phi ?c true false)" => "?c" ),


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

    // Constant Propagation
    rw!("prop-plus";    "(+ ?a ?b)"   => (ConstProp::new("?a", "?b", Box::new(Peg::plus)))),
    rw!("prop-equal";   "(== ?a ?b)"  => (ConstProp::new("?a", "?b", Box::new(Peg::equal)))),
    rw!("prop-nequal";  "(!= ?a ?b)"  => (ConstProp::new("?a", "?b", Box::new(Peg::nequal)))),
    rw!("prop-minus";   "(- ?a ?b)"   => (ConstProp::new("?a", "?b", Box::new(Peg::minus)))),
    rw!("prop-mult";    "(* ?a ?b)"   => (ConstProp::new("?a", "?b", Box::new(Peg::mult)))),
    rw!("prop-div";     "(/ ?a ?b)"   => (ConstProp::new("?a", "?b", Box::new(Peg::div)))),
    rw!("prop-and";     "(&& ?a ?b)"  => (ConstProp::new("?a", "?b", Box::new(Peg::and)))),
    rw!("prop-or";      "(|| ?a ?b)"  => (ConstProp::new("?a", "?b", Box::new(Peg::or)))),
    rw!("prop-bin-and"; "(& ?a ?b)"   => (ConstProp::new("?a", "?b", Box::new(Peg::bin_and)))),
    rw!("prop-bin-or";  "(| ?a ?b)"   => (ConstProp::new("?a", "?b", Box::new(Peg::bin_or)))),
    rw!("prop-lshift";  "(<< ?a ?b)"  => (ConstProp::new("?a", "?b", Box::new(Peg::lshift)))),
    rw!("prop-srshift"; "(>> ?a ?b)"  => (ConstProp::new("?a", "?b", Box::new(Peg::srshift)))),
    rw!("prop-urshift"; "(>>> ?a ?b)" => (ConstProp::new("?a", "?b", Box::new(Peg::urshift)))),
    rw!("prop-gt";  "(> ?a ?b)"  => (ConstProp::new("?a", "?b", Box::new(Peg::gt)))),
    rw!("prop-ge";  "(>= ?a ?b)" => (ConstProp::new("?a", "?b", Box::new(Peg::ge)))),
    rw!("prop-lt";  "(< ?a ?b)"  => (ConstProp::new("?a", "?b", Box::new(Peg::lt)))),
    rw!("prop-le";  "(<= ?a ?b)" => (ConstProp::new("?a", "?b", Box::new(Peg::le)))),

    // Equality Refinement
    // swap all a's for b's in t
    rw!("equality-refinement"; "(phi (== (var ?a) ?b) ?t ?e)" =>
        "(phi (== (var ?a) ?b) (swap (var ?a) ?b ?t) ?e)"),

    // Swapping
    rw!("swap-match";        "(swap (var ?a) ?b (var ?a))"         => "?b"),
    rw!("swap-var-pass-through"; "(swap (var ?a) ?b ?c)"           => "?c" if is_not_same_var("?a", "?c")),
    rw!("swap-const-pass-through"; "(swap (var ?a) ?b ?c)"         => "?c" if is_const("?c")),
    rw!("swap-or";   "(swap (var ?a) ?b (|| ?l ?r))"  => "(|| (swap (var ?a) ?b ?l) (swap (var ?a) ?b ?r))"),
    rw!("swap-and";  "(swap (var ?a) ?b (&& ?l ?r))"  => "(&& (swap (var ?a) ?b ?l) (swap (var ?a) ?b ?r))"),
    rw!("swap-plus"; "(swap (var ?a) ?b (+ ?l ?r))"   => "(+  (swap (var ?a) ?b ?l) (swap (var ?a) ?b ?r))"),
    rw!("swap-sub";  "(swap (var ?a) ?b (- ?l ?r))"   => "(-  (swap (var ?a) ?b ?l) (swap (var ?a) ?b ?r))"),
    rw!("swap-mul";  "(swap (var ?a) ?b (* ?l ?r))"   => "(*  (swap (var ?a) ?b ?l) (swap (var ?a) ?b ?r))"),
    rw!("swap-div";  "(swap (var ?a) ?b (/ ?l ?r))"   => "(/  (swap (var ?a) ?b ?l) (swap (var ?a) ?b ?r))"),
    rw!("swap-lt";   "(swap (var ?a) ?b (< ?l ?r))"   => "(<  (swap (var ?a) ?b ?l) (swap (var ?a) ?b ?r))"),
    rw!("swap-gt";   "(swap (var ?a) ?b (> ?l ?r))"   => "(>  (swap (var ?a) ?b ?l) (swap (var ?a) ?b ?r))"),
    rw!("swap-le";   "(swap (var ?a) ?b (<= ?l ?r))"  => "(<= (swap (var ?a) ?b ?l) (swap (var ?a) ?b ?r))"),
    rw!("swap-ge";   "(swap (var ?a) ?b (>= ?l ?r))"  => "(>= (swap (var ?a) ?b ?l) (swap (var ?a) ?b ?r))"),
    rw!("swap-neq";  "(swap (var ?a) ?b (!= ?l ?r))"  => "(!= (swap (var ?a) ?b ?l) (swap (var ?a) ?b ?r))"),
    rw!("swap-eq";   "(swap (var ?a) ?b (== ?l ?r))"  => "(== (swap (var ?a) ?b ?l) (swap (var ?a) ?b ?r))"),


    rw!("equality-distribute"; "(phi (== ?a ?b) ?t ?e)" =>
        "(phi (== ?a ?b) (eq-distr ?a ?b ?t) (neq-distr ?a ?b ?e))"),

    rw!("eq-distr-phi";  "(eq-distr ?a ?b (phi ?c ?t ?e))" => "(phi (eq-distr ?a ?b ?c) (eq-distr ?a ?b ?t) (eq-distr ?a ?b ?e))"),
    rw!("eq-distr-or";   "(eq-distr ?a ?b (|| ?l ?r))"  => "(|| (eq-distr ?a ?b ?l) (eq-distr ?a ?b ?r))"),
    rw!("eq-distr-and";  "(eq-distr ?a ?b (&& ?l ?r))"  => "(&& (eq-distr ?a ?b ?l) (eq-distr ?a ?b ?r))"),
    rw!("eq-distr-plus"; "(eq-distr ?a ?b (+ ?l ?r))"   => "(+  (eq-distr ?a ?b ?l) (eq-distr ?a ?b ?r))"),
    rw!("eq-distr-sub";  "(eq-distr ?a ?b (- ?l ?r))"   => "(-  (eq-distr ?a ?b ?l) (eq-distr ?a ?b ?r))"),
    rw!("eq-distr-mul";  "(eq-distr ?a ?b (* ?l ?r))"   => "(*  (eq-distr ?a ?b ?l) (eq-distr ?a ?b ?r))"),
    rw!("eq-distr-div";  "(eq-distr ?a ?b (/ ?l ?r))"   => "(/  (eq-distr ?a ?b ?l) (eq-distr ?a ?b ?r))"),
    rw!("eq-distr-lt";   "(eq-distr ?a ?b (< ?l ?r))"   => "(<  (eq-distr ?a ?b ?l) (eq-distr ?a ?b ?r))"),
    rw!("eq-distr-gt";   "(eq-distr ?a ?b (> ?l ?r))"   => "(>  (eq-distr ?a ?b ?l) (eq-distr ?a ?b ?r))"),
    rw!("eq-distr-le";   "(eq-distr ?a ?b (<= ?l ?r))"  => "(<= (eq-distr ?a ?b ?l) (eq-distr ?a ?b ?r))"),
    rw!("eq-distr-ge";   "(eq-distr ?a ?b (>= ?l ?r))"  => "(>= (eq-distr ?a ?b ?l) (eq-distr ?a ?b ?r))"),
    rw!("eq-distr-not";  "(eq-distr ?a ?b (! ?x))"      => "(!  (eq-distr ?a ?b ?x))"),
    rw!("eq-distr-eq";   "(eq-distr ?a ?b (== ?a ?b))"  => "true"),
    rw!("eq-distr-neq";  "(eq-distr ?a ?b (!= ?a ?b))"  => "false"),
    rw!("eq-dist-var";   "(eq-distr ?a ?b ?v)"           => "?v" if is_var("?v")),
    rw!("eq-dist-const"; "(eq-distr ?a ?b ?c)"           => "?c" if is_const("?c")),

    rw!("inequality-distribute"; "(phi (!= ?a ?b) ?t ?e)" =>
        "(phi (== ?a ?b) (neq-distr ?a ?b ?t) (eq-distr ?a ?b ?e))"),

    rw!("neq-distr-phi";  "(neq-distr ?a ?b (phi ?c ?t ?e))" => "(phi (neq-distr ?a ?b ?c) (neq-distr ?a ?b ?t) (neq-distr ?a ?b ?e))"),
    rw!("neq-distr-or";   "(neq-distr ?a ?b (|| ?l ?r))"  => "(|| (neq-distr ?a ?b ?l) (neq-distr ?a ?b ?r))"),
    rw!("neq-distr-and";  "(neq-distr ?a ?b (&& ?l ?r))"  => "(&& (neq-distr ?a ?b ?l) (neq-distr ?a ?b ?r))"),
    rw!("neq-distr-plus"; "(neq-distr ?a ?b (+ ?l ?r))"   => "(+  (neq-distr ?a ?b ?l) (neq-distr ?a ?b ?r))"),
    rw!("neq-distr-sub";  "(neq-distr ?a ?b (- ?l ?r))"   => "(-  (neq-distr ?a ?b ?l) (neq-distr ?a ?b ?r))"),
    rw!("neq-distr-mul";  "(neq-distr ?a ?b (* ?l ?r))"   => "(*  (neq-distr ?a ?b ?l) (neq-distr ?a ?b ?r))"),
    rw!("neq-distr-div";  "(neq-distr ?a ?b (/ ?l ?r))"   => "(/  (neq-distr ?a ?b ?l) (neq-distr ?a ?b ?r))"),
    rw!("neq-distr-lt";   "(neq-distr ?a ?b (< ?l ?r))"   => "(<  (neq-distr ?a ?b ?l) (neq-distr ?a ?b ?r))"),
    rw!("neq-distr-gt";   "(neq-distr ?a ?b (> ?l ?r))"   => "(>  (neq-distr ?a ?b ?l) (neq-distr ?a ?b ?r))"),
    rw!("neq-distr-le";   "(neq-distr ?a ?b (<= ?l ?r))"  => "(<= (neq-distr ?a ?b ?l) (neq-distr ?a ?b ?r))"),
    rw!("neq-distr-ge";   "(neq-distr ?a ?b (>= ?l ?r))"  => "(>= (neq-distr ?a ?b ?l) (neq-distr ?a ?b ?r))"),
    rw!("neq-distr-not";  "(neq-distr ?a ?b (! ?x))"      => "(!  (neq-distr ?a ?b ?x))"),
    rw!("neq-distr-neq";  "(neq-distr ?a ?b (!= ?a ?b))"  => "true"),
    rw!("neq-distr-eq";   "(neq-distr ?a ?b (== ?a ?b))"  => "false"),
    rw!("neq-dist-var";   "(neq-distr ?a ?b ?v)"          => "?v" if is_var("?v")),
    rw!("neq-dist-const"; "(neq-distr ?a ?b ?c)"          => "?c" if is_const("?c")),

  ];
  Box::new(rules)
}

struct ConstProp {
  left: Var,
  right: Var,
  op: Box<dyn Fn(&Peg, &Peg) -> Peg>,
}

impl ConstProp {
  fn new(left: &str, right: &str, op: Box<dyn Fn(&Peg, &Peg) -> Peg>) -> Self {
    ConstProp {
      left: left.parse().unwrap(),
      right: right.parse().unwrap(),
      op,
    }
  }
}

impl Applier<Peg, VarAnalysis> for ConstProp {
  fn apply_one(&self, egraph: &mut EGraph, _: Id, subst: &Subst) -> Vec<Id> {
    let left_class = egraph[subst[self.left]].clone();
    let right_class = egraph[subst[self.right]].clone();
    let mut result = vec![];
    for left in left_class
      .nodes
      .iter()
      .filter(|n| n.is_const())
      .map(|n| n.clone())
    {
      for right in right_class
        .nodes
        .iter()
        .filter(|n| n.is_const())
        .map(|n| n.clone())
      {
        let id = egraph.add((self.op)(&left, &right));
        result.push(id);
      }
    }
    result
  }
}

pub fn subjects() -> Result<Vec<Subject>, String> {
  Subject::from_file("xmls/subjects.xml".to_string())
}

impl Peg {
  pub fn is_const(&self) -> bool {
    use Peg::*;
    match self {
      Num(_) | Bool(_) => true,
      _ => false,
    }
  }

  pub fn is_var(&self) -> bool {
    use Peg::*;
    match self {
      Var(_) => true,
      _ => false,
    }
  }

  pub fn is_num_binop(&self) -> bool {
    use Peg::*;
    match self {
      Add(..) | Mul(..) | Div(..) | Sub(..) | BinOr(..) | BinAnd(..) | LShift(..) | URShift(..) | SRShift(..) => true,
      _ => false,
    }
  }

  pub fn is_bool_binop(&self) -> bool {
    use Peg::*;
    match self {
      And(..) | Or(..) => true,
      _ => false,
    }
  }

  pub fn is_binop(&self) -> bool {
    self.is_bool_binop() || self.is_num_binop()
  }

  pub fn is_ground(&self) -> bool {
    match self {
      Peg::Symbol(_) => true,
      _ => self.is_const(),
    }
  }

  pub fn plus(a: &Peg, b: &Peg) -> Peg {
    use Peg::*;
    match (a, b) {
      (Num(a), Num(b)) => Num(a + b),
      _ => panic!(),
    }
  }

  pub fn equal(a: &Peg, b: &Peg) -> Peg {
    use Peg::*;
    match (a, b) {
      (Num(a),  Num(b))  => Bool(a == b),
      (Bool(a), Bool(b)) => Bool(a == b),
      _ => panic!(),
    }
  }

  pub fn nequal(a: &Peg, b: &Peg) -> Peg {
    use Peg::*;
    match (a, b) {
      (Num(a),  Num(b))  => Bool(a != b),
      (Bool(a), Bool(b)) => Bool(a != b),
      _ => panic!(),
    }
  }

  pub fn lt(a: &Peg, b: &Peg) -> Peg {
    use Peg::*;
    match (a, b) {
      (Num(a), Num(b)) => Bool(a < b),
      _ => panic!(),
    }
  }

  pub fn le(a: &Peg, b: &Peg) -> Peg {
    use Peg::*;
    match (a, b) {
      (Num(a), Num(b)) => Bool(a <= b),
      _ => panic!(),
    }
  }

  pub fn gt(a: &Peg, b: &Peg) -> Peg {
    use Peg::*;
    match (a, b) {
      (Num(a), Num(b)) => Bool(a > b),
      _ => panic!(),
    }
  }

  pub fn ge(a: &Peg, b: &Peg) -> Peg {
    use Peg::*;
    match (a, b) {
      (Num(a), Num(b)) => Bool(a >= b),
      _ => panic!(),
    }
  }

  pub fn minus(a: &Peg, b: &Peg) -> Peg {
    use Peg::*;
    match (a, b) {
      (Num(a), Num(b)) => Num(a - b),
      _ => panic!(),
    }
  }

  pub fn mult(a: &Peg, b: &Peg) -> Peg {
    use Peg::*;
    match (a, b) {
      (Num(a), Num(b)) => Num(a * b),
      _ => panic!(),
    }
  }

  pub fn div(a: &Peg, b: &Peg) -> Peg {
    use Peg::*;
    match (a, b) {
      (Num(a), Num(b)) => Num(a / b),
      _ => panic!(),
    }
  }

  pub fn bin_and(a: &Peg, b: &Peg) -> Peg {
    use Peg::*;
    match (a, b) {
      (Num(a), Num(b)) => Num(a & b),
      _ => panic!(),
    }
  }

  pub fn bin_or(a: &Peg, b: &Peg) -> Peg {
    use Peg::*;
    match (a, b) {
      (Num(a), Num(b)) => Num(a | b),
      _ => panic!(),
    }
  }

  pub fn srshift(a: &Peg, b: &Peg) -> Peg {
    use Peg::*;
    match (a, b) {
      (Num(a), Num(b)) => {
        let a = *a as u32;
        let b = *b as u32;
        Num((a >> b) as i32)
      },
      _ => panic!(),
    }
  }

  pub fn urshift(a: &Peg, b: &Peg) -> Peg {
    use Peg::*;
    match (a, b) {
      (Num(a), Num(b)) => Num(a >> b),
      _ => panic!(),
    }
  }

  pub fn lshift(a: &Peg, b: &Peg) -> Peg {
    use Peg::*;
    match (a, b) {
      (Num(a), Num(b)) => Num(a << b),
      _ => panic!(),
    }
  }

  pub fn and(a: &Peg, b: &Peg) -> Peg {
    use Peg::*;
    match (a, b) {
      (Bool(a), Bool(b)) => Bool(*a && *b),
      _ => panic!(),
    }
  }

  pub fn or(a: &Peg, b: &Peg) -> Peg {
    use Peg::*;
    match (a, b) {
      (Bool(a), Bool(b)) => Bool(*a || *b),
      _ => panic!(),
    }
  }
}

#[derive(Debug, Deserialize)]
struct Subjects {
  subject: Vec<Subject>,
}

#[derive(Debug, Deserialize)]
pub struct Subject {
  #[serde(rename = "sourcefile")]
  pub source_file: String,
  pub method: String,
  #[serde(rename = "egg")]
  pub code: Code,
  #[serde(rename = "mutant")]
  pub mutants: Vec<Mutant>,
}

impl Subject {
  pub fn make(source_file: String, method: String, inputs: &[(u32, &str)]) -> Subject {
    let mut mutants = Vec::new();

    for (id, src) in inputs[1..].iter() {
      let m = Mutant {
        id: *id,
        code: Code::new(String::from(*src)),
      };
      mutants.push(m);
    }

    Subject {
      source_file,
      method,
      code: Code::new(String::from(inputs[0].1)),
      mutants,
    }
  }

  pub fn from_file(path: String) -> Result<Vec<Subject>, String> {
    use std::fs;
    println!("Reading from path {}", path);
    let contents = fs::read_to_string(path).unwrap();
    let parsed: Subjects = from_reader(contents.as_bytes()).unwrap();
    Ok(parsed.subject)
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
  pub code: Code,
}
