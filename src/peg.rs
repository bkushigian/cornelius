use egg::*;

pub type EGraph = egg::EGraph<Peg, VarAnalysis>;

define_language! {
  pub enum Peg {
    Num(i32),
    Bool(bool),
    "error" = Error,
    "+"    = Add([Id; 2]),
    "*"    = Mul([Id; 2]),
    "/"    = Div([Id; 2]),
    "-"    = Sub([Id; 2]),
    "---"  = Neg(Id),
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
    "cond-distr" = EqDistr([Id; 2]),
    "var"  = Var(Id),
    Symbol(egg::Symbol),

    /***                       Heapy Stuff                       ***/

    // An access path consists of a base node and a chain of derefs
    "path" = AccessPath([Id; 2]),
    // (derefs ".a.b.c")
    // derefs consists of a string (Symbol) representation of a chain of derefs
    // ".a.b.c"
    "derefs" = Derefs(Id),
    // a heap represents an unknown heap state. it is indexed to syntactically
    // differentiate unknown heaps
    "heap" = Heap(Id),
    // (wr path value heap)
    // represent the heap `heap'` that is equal to `heap` at all points save for
    // `path`, which now has value `value`
    "wr" = Wr([Id; 3]),
    // (rd path heap)
    // read the value stored at an access path `path` in a heap `heap`
    "rd" = Rd([Id; 2]),
  }
}

pub fn is_const(v1: &'static str) -> impl Fn(&mut EGraph, Id, &Subst) -> bool {
    let v1: egg::Var = v1.parse().unwrap();
    move |egraph, _, subst| egraph[subst[v1]].data.constant.is_some()
}

pub fn is_var(v1: &'static str) -> impl Fn(&mut EGraph, Id, &Subst) -> bool {
    let v1: egg::Var = v1.parse().unwrap();
    move |egraph, _, subst| egraph[subst[v1]].data.variable.is_some()
}

pub fn is_not_same_var(v1: &'static str, v2: &'static str) -> impl Fn(&mut EGraph, Id, &Subst) -> bool {
    let v1: egg::Var = v1.parse().unwrap();
    let v2: egg::Var = v2.parse().unwrap();
    move |egraph: &mut EGraph, _, subst: &Subst| egraph.find(subst[v1]) != egraph.find(subst[v2])
}

impl Peg {
  pub fn as_int(&self) -> Option<i32> {
    match self {
      Peg::Num(n) => Some(*n),
      _ => None
    }
  }

  pub fn as_bool(&self) -> Option<bool> {
    match self {
      Peg::Bool(b) => Some(*b),
      _ => None
    }
  }

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

/// Data tracked for a VarAnalysis
#[derive(Default, PartialEq, Debug, Clone)]
pub struct VarAnalysisData {
  pub constant: Option<Peg>,
  pub variable: Option<Peg>
}

impl VarAnalysisData {
  pub fn or(self, a: VarAnalysisData) -> VarAnalysisData {
    VarAnalysisData {
      constant: self.constant.or(a.constant),
      variable: self.variable.or(a.variable)
    }
  }
}

#[derive(Default)]
pub struct VarAnalysis;

fn eval(egraph: &EGraph, enode: &Peg) -> Option<Peg> {
  let x = |i: &Id| egraph[*i].data.constant.clone();

  match enode {
    // Arithmetic
    Peg::Num(_) | Peg::Bool(_) => Some(enode.clone()),
    Peg::Add([a, b]) => Some(Peg::Num(x(a)?.as_int()? + x(b)?.as_int()?)),
    Peg::Sub([a, b]) => Some(Peg::Num(x(a)?.as_int()? - x(b)?.as_int()?)),
    Peg::Mul([a, b]) => Some(Peg::Num(x(a)?.as_int()? * x(b)?.as_int()?)),
    Peg::Div([a, b]) => {
      let d = x(b)?.as_int()?;
      if d == 0 {
        Some(Peg::Error)
      } else {
        Some(Peg::Num(x(a)?.as_int()? / d))
      }
    },

    Peg::Neg(a) => Some(Peg::Num(-x(a)?.as_int()?)),

    // Comparison
    Peg::Gte([a, b]) => Some(Peg::Bool(x(a)?.as_int()? >= x(b)?.as_int()?)),
    Peg::Gt([a, b])  => Some(Peg::Bool(x(a)?.as_int()? >  x(b)?.as_int()?)),
    Peg::Lte([a, b]) => Some(Peg::Bool(x(a)?.as_int()? <= x(b)?.as_int()?)),
    Peg::Lt([a, b])  => Some(Peg::Bool(x(a)?.as_int()? <  x(b)?.as_int()?)),

    Peg::Equ([a, b]) => {
      // To check for equality,
      let a = x(a)?;
      let b = x(b)?;
      if a.is_const() && b.is_const() {
        // FIXME Is this correct? In particular, will this ever return false
        // when it should return true?
        Some(Peg::Bool(a == b))
      } else {
        None
      }
    }
    Peg::Neq([a, b]) => {
      // To check for equality,
      let a = x(a)?;
      let b = x(b)?;
      if a.is_const() && b.is_const() {
        // FIXME Is this correct? In particular, will this ever return false
        // when it should return true?
        Some(Peg::Bool(a != b))
      } else {
        None
      }
    }

    _ => None,
  }
}

impl Analysis<Peg> for VarAnalysis {
    type Data = VarAnalysisData;
    fn merge(&self, to: &mut Self::Data, from: Self::Data) -> bool {
      merge_if_different(to, to.clone().or(from))
    }

    fn make(egraph: &EGraph, enode: &Peg) -> Self::Data {
        let _x = |i: &Id| egraph[*i].data.clone();
        match enode {
          Peg::Var(_) => VarAnalysisData {constant: None, variable: Some(enode.clone())},
          _ => VarAnalysisData {constant: eval(egraph, enode), variable: None}
        }
    }

    fn modify(egraph: &mut EGraph, id: Id) {
        if let Some(c) = egraph[id].data.constant.clone() {
            let const_id = egraph.add(c);
            egraph.union(id, const_id);
        }
    }
}
