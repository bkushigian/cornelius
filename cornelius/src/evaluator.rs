use crate::peg::Peg;
use egg::{RecExpr, Id};
use std::collections::HashMap;

#[allow(dead_code)]
pub fn eval(expr: &RecExpr<Peg>, var_bindings: HashMap<&str, Peg>) -> Result<Peg, String>{
    // set up bindings
    let mut bindings: HashMap<Id, Peg> = HashMap::default();
    let expr_ref = expr.as_ref();

    // iterate through each peg and its index in the rec_expr and find all vars
    // If they have a name in `var_bindings`, transform their index in the RecExpr
    // into an egg::Id and use this as a lookup in bindings
    for peg in expr_ref {
        if let Peg::Var(id) = peg {
            println!("peg={:?}", peg);
            // if peg is is a var, update bindings
            if let Some(Peg::Symbol(name)) = expr_ref.get(usize::from(*id)) {
                match var_bindings.get(name.as_str()) {
                    Some(Peg::Num(n)) => {
                        println!("inserting Peg::Num({}) at id={}", n, id);
                        bindings.insert(*id, Peg::Num(*n));
                    }
                    Some(Peg::Bool(b)) => {
                        bindings.insert(*id, Peg::Bool(*b));
                    }
                    Some(Peg::Null) => {
                        bindings.insert(*id, Peg::Null);
                    }
                    Some(Peg::Unit) => {
                        bindings.insert(*id, Peg::Unit);
                    }
                    _ => (),
                }
            };
        }
    }

    let mut expr = expr.clone();
    Evaluator{
        expr: &mut expr,
        bindings,
        memoize:HashMap::default(),
        code_table: HashMap::default(),
    }.evaluate_expr_at(expr_ref.len() - 1)
}

pub struct Evaluator<'a> {
    /// The expression we are evaluating. If new terms are needed, they are
    /// added to this
    expr: &'a mut RecExpr<Peg>,
    /// Initial Bindings from vars to `Peg`s. Currently each stored Peg should
    /// be a grounded term
    bindings: HashMap<Id, Peg>,
    /// A memoization table that stores computed values for each `Id`. This
    /// gives us performance gains from deduplicated nodes.
    memoize: HashMap<Id, Peg>,
    /// Callable methods in `expr`
    #[allow(dead_code)]
    code_table: HashMap<&'a str, Id>,
}

impl<'a> Evaluator<'a> {
    pub fn evaluate_expr_at(&mut self, idx: usize) -> Result<Peg, String> {
        use crate::peg::Peg::*;
        let root = self.expr.as_ref().get(idx).expect("index out of bounds").clone();
        let id = Id::from(idx);
        if self.memoize.contains_key(&id) {
            return Ok(self.memoize.get(&id).unwrap().clone());
        }
        let peg = match root {
            Num(n)     => Ok(Num(n)),
            Bool(b)    => Ok(Bool(b)),
            Error      => Ok(Error),
            Null       => Ok(Null),
            Add([l, r]) => {
                let lhs = self.evaluate_expr_at(usize::from(l))?;
                let rhs = self.evaluate_expr_at(usize::from(r))?;
                match (lhs, rhs) {
                    (Num(x), Num(y)) => Ok(Num(x + y)),
                    _ => Err("Arguments of add node didn't evaluate to ground term".to_owned()),
                }
            },
            Mul([l, r]) => {
                let lhs = self.evaluate_expr_at(usize::from(l))?;
                let rhs = self.evaluate_expr_at(usize::from(r))?;
                match (lhs, rhs) {
                    (Num(x), Num(y)) => Ok(Num(x * y)),
                    _ => Err("Arguments of mul node didn't evaluate to ground term".to_owned()),
                }
            },
            Div([l, r]) => {
                let lhs = self.evaluate_expr_at(usize::from(l))?;
                let rhs = self.evaluate_expr_at(usize::from(r))?;
                match (lhs, rhs) {
                    // TODO handle div by zero
                    (Num(x), Num(y)) => Ok(Num(x / y)),
                    _ => Err("Arguments of div node didn't evaluate to ground term".to_owned()),
                }
            },
            Sub([l, r]) => {
                let lhs = self.evaluate_expr_at(usize::from(l))?;
                let rhs = self.evaluate_expr_at(usize::from(r))?;
                match (lhs, rhs) {
                    (Num(x), Num(y)) => Ok(Num(x - y)),
                    _ => Err("Arguments of sub node didn't evaluate to ground term".to_owned()),
                }
            },
            And([l, r]) => {
                let lhs = self.evaluate_expr_at(usize::from(l))?;
                let rhs = self.evaluate_expr_at(usize::from(r))?;
                match (lhs, rhs) {
                    (Bool(x), Bool(y)) => Ok(Bool(x && y)),
                    _ => Err("Arguments of and node didn't evaluate to ground term".to_owned()),
                }
            },
            Or([l, r]) => {
                let lhs = self.evaluate_expr_at(usize::from(l))?;
                let rhs = self.evaluate_expr_at(usize::from(r))?;
                match (lhs, rhs) {
                    (Bool(x), Bool(y)) => Ok(Bool(x || y)),
                    _ => Err("Arguments of || node didn't evaluate to ground term".to_owned()),
                }
            },
            BinAnd([l, r]) => {
                let lhs = self.evaluate_expr_at(usize::from(l))?;
                let rhs = self.evaluate_expr_at(usize::from(r))?;
                match (lhs, rhs) {
                    // TODO handle div by zero
                    (Num(x), Num(y)) => Ok(Num(x & y)),
                    _ => Err("Arguments of bin-and node didn't evaluate to ground term".to_owned()),
                }
            },
            BinOr([l, r]) => {
                let lhs = self.evaluate_expr_at(usize::from(l))?;
                let rhs = self.evaluate_expr_at(usize::from(r))?;
                match (lhs, rhs) {
                    (Num(x), Num(y)) => Ok(Num(x | y)),
                    _ => Err("Arguments of bin-or node didn't evaluate to ground term".to_owned()),
                }
            },
            Xor([l, r]) => {
                let lhs = self.evaluate_expr_at(usize::from(l))?;
                let rhs = self.evaluate_expr_at(usize::from(r))?;
                match (lhs, rhs) {
                    (Num(x), Num(y)) => Ok(Num(x ^ y)),
                    (Bool(x), Bool(y)) => Ok(Bool(x ^ y)),
                    _ => Err("Arguments of bin-or node didn't evaluate to ground term".to_owned()),
                }
            },

            SRShift([l, r]) => {
                let lhs = self.evaluate_expr_at(usize::from(l))?;
                let rhs = self.evaluate_expr_at(usize::from(r))?;
                match (lhs, rhs) {
                    (Num(x), Num(y)) => Ok(Num(x >> y)),
                    _ => Err("Arguments of srshift node didn't evaluate to ground term".to_owned()),
                }
            },

            URShift([l, r]) => {
                let lhs = self.evaluate_expr_at(usize::from(l))?;
                let rhs = self.evaluate_expr_at(usize::from(r))?;
                match (lhs, rhs) {
                    (Num(x), Num(y)) => Ok(Num(((x as u32) >> y) as i32)),
                    _ => Err("Arguments of urshift node didn't evaluate to ground term".to_owned()),
                }
            },

            LShift([l, r]) => {
                let lhs = self.evaluate_expr_at(usize::from(l))?;
                let rhs = self.evaluate_expr_at(usize::from(r))?;
                match (lhs, rhs) {
                    (Num(x), Num(y)) => Ok(Num(x << y)),
                    _ => Err("Arguments of lshift node didn't evaluate to ground term".to_owned()),
                }
            },

            Not(id) => {
                let arg = self.evaluate_expr_at(usize::from(id))?;
                match arg {
                    Bool(x) => Ok(Bool(!x)),
                    _ => Err("Arguments of not node didn't evaluate to ground term".to_owned()),
                }
            },

            Lt([l, r]) => {
                let lhs = self.evaluate_expr_at(usize::from(l))?;
                let rhs = self.evaluate_expr_at(usize::from(r))?;
                match (lhs, rhs) {
                    (Num(x), Num(y)) => Ok(Bool(x < y)),
                    _ => Err("Arguments of lt node didn't evaluate to ground term".to_owned()),
                }
            },

            Lte([l, r]) => {
                let lhs = self.evaluate_expr_at(usize::from(l))?;
                let rhs = self.evaluate_expr_at(usize::from(r))?;
                match (lhs, rhs) {
                    (Num(x), Num(y)) => Ok(Bool(x <= y)),
                    _ => Err("Arguments of lte node didn't evaluate to ground term".to_owned()),
                }
            },


            Gt([l, r]) => {
                let lhs = self.evaluate_expr_at(usize::from(l))?;
                let rhs = self.evaluate_expr_at(usize::from(r))?;
                match (lhs, rhs) {
                    (Num(x), Num(y)) => Ok(Bool(x > y)),
                    _ => Err("Arguments of gt node didn't evaluate to ground term".to_owned()),
                }
            },

            Gte([l, r]) => {
                let lhs = self.evaluate_expr_at(usize::from(l))?;
                let rhs = self.evaluate_expr_at(usize::from(r))?;
                match (lhs, rhs) {
                    (Num(x), Num(y)) => Ok(Bool(x >= y)),
                    _ => Err("Arguments of gte node didn't evaluate to ground term".to_owned()),
                }
            },
            Equ([l, r]) => {
                let lhs = self.evaluate_expr_at(usize::from(l))?;
                let rhs = self.evaluate_expr_at(usize::from(r))?;
                match (lhs, rhs) {
                    (Num(x), Num(y)) => Ok(Bool(x == y)),
                    (Bool(x), Bool(y)) => Ok(Bool(x == y)),
                    _ => Ok(Bool(false)),
                }
            },
            Neq([l, r]) => {
                let lhs = self.evaluate_expr_at(usize::from(l))?;
                let rhs = self.evaluate_expr_at(usize::from(r))?;
                match (lhs, rhs) {
                    (Num(x), Num(y)) => Ok(Bool(x != y)),
                    (Bool(x), Bool(y)) => Ok(Bool(x != y)),
                    _ => Ok(Bool(true)),
                }
            },
            Phi([c, t, e]) => {
                let cond = self.evaluate_expr_at(usize::from(c))?;
                match cond {
                    Bool(true) => self.evaluate_expr_at(usize::from(t)),
                    Bool(false) => self.evaluate_expr_at(usize::from(e)),
                    _ => Err("Condition of phi node didn't evaluate to a ground boolean term".to_owned())
                }
            },
            Var(id) => {
                Ok(self.bindings.get(&id)
                   .ok_or(format!("Var (id={}) was not found in lookup\nbindings={:?}", id, self.bindings))?.clone())
            },
            AccessPath(_) => Err("Cannot evaluate access path directly".to_owned()),
            Derefs(_) => Err("Cannot evaluate derefs directly".to_owned()),
            Heap(_) => Err("Cannot evaluate heap directly".to_owned()),
            Wr(_) => Err("Cannot evaluate wr node directly directly".to_owned()),
            Rd(_) => Err("Cannot evaluate rd node directly directly".to_owned()),
            MethodRoot([val, heap]) => {
                let r = self.evaluate_expr_at(usize::from(val))?;
                let ret_val_id = self.expr.add(r);
                let meth_root = MethodRoot([ret_val_id, heap]);
                self.expr.add(meth_root.clone());
                Ok(meth_root)
            },
            Invoke([_heap, _receiver, _method, _actuals]) => {
                panic!("Invocations are not implemented yet");
            }

            _ => panic!("todo")
        };
        let id = Id::from(idx);
        self.memoize.insert(id, peg.clone()?);
        peg
    }
}


#[cfg(test)]
mod tests {
    use super::*;
    use crate::map;

    const PLUS_A_2: &str = "(+ (var a) 2)";
    const PLUS_1_3_2: &str = "(+ (+ 1 3) 2)";
    const PLUS_A_B_C: &str = "(+ (+ 1 3) 2)";
    const MIN: &str = "(phi (< (var a) (var b)) (var a) (var b))";
    const MAX: &str = "(phi (> (var a) (var b)) (var a) (var b))";
    const MIN_OF_3: &str = "(phi (< (var a) (var b))
                               (phi (< (var a) (var c))
                                    (var a)
                                    (var c))
                               (phi (< (var b) (var c)) (var b) (var c)))";

    #[test]
    fn eval_plus() -> Result<(), String>{
        let env = map!{"a" => Peg::Num(1), "b" => Peg::Num(2), "c" => Peg::Num(3)};
        assert_eq!(eval(&PLUS_A_2.parse().unwrap(), env.clone())?, Peg::Num(3));
        assert_eq!(eval(&PLUS_1_3_2.parse().unwrap(), env.clone())?, Peg::Num(6));
        assert_eq!(eval(&PLUS_A_B_C.parse().unwrap(), env.clone())?, Peg::Num(6));
        Ok(())
    }

    #[test]
    fn eval_phi() -> Result<(), String> {
        let env = map!{"a" => Peg::Num(1), "b" => Peg::Num(2), "c" => Peg::Num(3)};
        assert_eq!(eval(&MIN.parse().unwrap(), env.clone())?,      Peg::Num(1));
        assert_eq!(eval(&MAX.parse().unwrap(), env.clone())?,      Peg::Num(2));
        assert_eq!(eval(&MIN_OF_3.parse().unwrap(), env.clone())?, Peg::Num(1));

        let env = map!{"a" => Peg::Num(11), "b" => Peg::Num(-2), "c" => Peg::Num(3)};
        assert_eq!(eval(&MIN.parse().unwrap(), env.clone())?,      Peg::Num(-2));
        assert_eq!(eval(&MAX.parse().unwrap(), env.clone())?,      Peg::Num(11));
        assert_eq!(eval(&MIN_OF_3.parse().unwrap(), env.clone())?, Peg::Num(-2));
        Ok(())
    }
}
