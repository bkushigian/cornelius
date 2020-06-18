use crate::expr::{Peg, VarAnalysis, RewriteSystem};
use egg::Rewrite;

pub struct RWRule(Rewrite<Peg, VarAnalysis>);

pub type Delta<'a>  = Vec<&'a RWRule>;
pub type Deltas<'a> = Vec<Delta<'a>>;

impl PartialEq for RWRule {
    fn eq(&self, other: &Self) -> bool {
        self.0.name() == other.0.name() && self.0.long_name() == other.0.long_name()
    }
}

impl Eq for RWRule {}

/// minimize_buggy_rules
pub fn minimize_buggy_rules(
    rules: &RewriteSystem,
    oracle: fn (&Deltas) -> bool
) -> &RewriteSystem {
    panic!("Not implemented")
}

/// Given a set of good circumstances and a set of breaking deltas that breaks
pub fn check_positive_deltas<'a>(
    c_good: Delta<'a>,
    d_bad: Deltas,
    oracle: fn (&Delta) -> bool
) -> Option<Delta<'a>> {
    let mut ds = d_bad.clone();
    for delta in d_bad {
        ds.append(delta);
    }
    None
}

pub fn split_into_n_deltas(deltas: Deltas, n: u32) {
}
// /// Represents the deltas of a rewrite system
