#![allow(dead_code)]
#![allow(unused_imports)]
#![allow(unused_variables)]
use crate::expr::{Peg, VarAnalysis, RewriteSystem};
use egg::{Rewrite, rewrite as rw};

/// ddmin2
/// Takes a bad configuration, a granularity, and an oracle that identifies good
/// configurations (i.e., returns `true` when the configuration is good, and
/// `false` when the configuration is bad). Returns a 1-minimal configuration
/// that fails the oracle.
fn ddmin2<'a, T>(
    bad_config: &[&'a T],
    granularity: usize,
    mut oracle: impl FnMut (&[&'a T]) -> bool
) -> Vec<&'a T>
where T: std::fmt::Debug {

    if granularity > bad_config.len() {
        panic!("Violation if recursion invariant: configs length less than split size")
    }

    let batch_size = bad_config.len() / granularity;
    let deltas: Vec<_> = bad_config.chunks(batch_size).collect();
    let granularity = deltas.len();

    for delta in &deltas {
        if !oracle(delta) {
            if delta.len() == 1 {
                return delta.to_vec();
            }
            return ddmin2(delta, 2, oracle);
        }
    }

    for i in 0..granularity {
        let mut deltas = deltas.clone();
        deltas.remove(i);
        let nabla = deltas.concat();
        if !oracle(&nabla) {
            return ddmin2(&nabla, 2.max(granularity-1), oracle);
        }
    }

    if granularity < bad_config.len() {
        return ddmin2(bad_config, (2 * granularity).min(bad_config.len()), oracle);
    }
    bad_config.to_vec()
}

pub fn dd<'a, T>(
    bad_config: &[&'a T],
    oracle: impl FnMut (&[&'a T]) -> bool
) -> Vec<&'a T>
where T: std::fmt::Debug {
    ddmin2(bad_config, 2, oracle)
}

/// minimize_buggy_rules
pub fn minimize_buggy_rules<'a>(
    rules: &[&'a Rewrite<Peg, VarAnalysis>],
    oracle: impl FnMut (&[&'a Rewrite<Peg, VarAnalysis>]) -> bool
) -> Vec<&'a Rewrite<Peg, VarAnalysis>> {
    ddmin2(rules, 2, oracle)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test1() {
        // doesn't contain 0
        let oracle = |xs: &[&i32]| !xs.contains(&&0);

        let config: Vec<i32>  = vec![0, 1, 2, 3, 4];
        let config: Vec<&i32> = config.iter().collect();

        let min = dd(config.as_slice(), oracle);
        assert!(min.len() == 1);
        assert!(min.contains(&&0));

        let config: Vec<i32>  = vec![0, 0, 0, 0, 0];
        let config: Vec<&i32> = config.iter().collect();

        let min = dd(config.as_slice(), oracle);
        assert!(min.len() == 1);
        assert!(min.contains(&&0));

    }

    #[test]
    fn test2() {
        // all even
        let oracle = |xs: &[&i32]| xs.iter().all(|x| **x % 2 == 0);

        let config: Vec<i32>  = vec![0, 1, 2, 4, 6, 8, 10, 12, 1, 1, 1];
        let config: Vec<&i32> = config.iter().collect();

        let min = dd(config.as_slice(), oracle);
        assert!(min.len() == 1);
        assert!(min.get(0) == Some(&&1));
    }

    #[test]
    fn test3() {
        let oracle = |xs: &[&i32]| !(xs.contains(&&0) && xs.contains(&&1));

        let config: Vec<i32>  = vec![0, 1, 2, 4, 6, 8, 10, 12];
        let config: Vec<&i32> = config.iter().collect();

        let min = dd(config.as_slice(), oracle);
        assert!(min.len() == 2);
        assert!(min.contains(&&0));
        assert!(min.contains(&&1));
    }

    #[test]
    fn test4() {
        let oracle = |xs: &[&i32]| {
            let mut sum = 0;
            for x in xs {
                sum += **x;
            }
            sum == 0
        };

        let config: Vec<i32>  = vec![-1, -2, 3];
        let config: Vec<&i32> = config.iter().collect();

        let min = dd(config.as_slice(), oracle);
        assert!(min.len() == 1);
    }
}
