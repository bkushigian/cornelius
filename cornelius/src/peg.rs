use egg::*;
use crate::primitives::{JavaLong, JavaInt, IsZero};

pub type EGraph = egg::EGraph<Peg, PegAnalysis>;

define_language! {
  pub enum Peg {
    Num(JavaInt),
    Long(JavaLong),
    Bool(bool),
    // A generic error. This is a stand in for all exceptional behavior and is
    // unsound.
    // TODO: handle exceptions explicitly
    "error" = Error,
    // The return value of void methods (can never actually use this in
    // computation)
    "unit" = Unit,
    // Here I reintroduce null values to Rust
    "null" = Null,

    "isnull?" = IsNull(Id),
    "isunit?" = IsUnit(Id),
    "+"    = Add([Id; 2]),
    "*"    = Mul([Id; 2]),
    "/"    = Div([Id; 2]),
    "%"    = Rem([Id; 2]),
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
    "_++"  = PostInc(Id),
    "_--"  = PostDec(Id),
    "++_"  = PreInc(Id),
    "--_"  = PreDec(Id),
    "phi"  = Phi([Id; 3]),
    "var"  = Var([Id; 2]),
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
    "heap" = Heap([Id; 2]),
    // (wr path value heap)
    // represent the heap `heap'` that is equal to `heap` at all points save for
    // `path`, which now has value `value`
    "wr" = Wr([Id; 3]),
    // (rd path heap)
    // read the value stored at an access path `path` in a heap `heap`
    "rd" = Rd([Id; 2]),
    // (ReturnNode return heap)
    // represent the returned value from a mutant (or original program)---this
    // is used as the root of a program we are comparing for equality
    "return-node" = ReturnNode([Id; 2]),

    /***                       Method Stuff                      ***/

    // (invoke heap receiver method actuals)
    // A normal (i.e., non-static) method invocation
    "invoke" = Invoke([Id; 4]),
    // Get the heap state from an invoke node
    "invoke->heap-state" = InvokeToHeapState(Id),
    // Get the exception status from an invoke node
    "invoke->exception-status" = InvokeToExceptionStatus(Id),
    // Get the returned value of an invoke node
    "invoke->peg" = InvokeToPeg(Id),
    // (invoke-static heap method actuals)
    // A static method invocation
    "invoke-static" = InvokeStatic([Id; 3]),
    // Method name that is being invoked
    "method" = MethodName(Id),
    // Actual paramters passed to a method
    "actuals" = Actuals(Box<[Id]>),
    // A list of exit conditions: these are not rewritten for now to avoid AC
    // blowup
    "exit-conditions" = ExitCondition(Box<[Id]>),

    /***                          Cast Stuff                     ***/

    // (can-cast? obj type-name)
    // This node represents the boolean predicate "obj can legally be cast to
    // type-name"
    "can-cast?" = CanCast([Id; 2]),
    // (cast obj type-name)
    "cast" = Cast([Id; 2]),
    // (type-name name)
    "type-name" = TypeName(Id),

    /***                       Object Creation                  ***/
    // (new type actuals heap)
    //
    // Represent a `new ...` expression.
    // + `type`: is the name of the constructor
    // + `actuals`: an (actuals id1 id2 ...) node representing the actual
    //              arguments passed to the constructor
    // + `heap` is the heap in which object creation takes place.
    "new" = New([Id; 3]),

    /***                         Loops                          ***/

    // Theta nodes, represents a sequence of values
    "theta" = Theta([Id; 2]),

    // A blank node, this is a place holder.
    "blank" = Blank(Id),

    // A pass node returns the index of the first false value in a sequence
    "pass" = Pass(Id),

    // Evaluate a theta node at a given iteration.
    //
    // (eval THETA N) produces the Nth value of the sequence
    // THETA.
    "eval" = Eval([Id; 2]),

    /***                   Maximal Expression Stuff           ***/
    // A MaxExpr represents a maximal expression that was mutated (it is not a
    // proper subexpression). To disambiguate different maximal expressions we
    // pass in a line/column number string "lineno:colno" as the first arg, the
    // peg id of the actual expression as the second argument, the resulting
    // context as the third argument, and the resulting heap as the final
    // argument.
    //
    // We've reified contexts as peg nodes to track changes in local state.
    "max-expr" = MaxExpr([Id; 4]),

    "ctx-nil" = ContextNil,

    "ctx-cons" = ContextCons([Id; 3]),

    /***                  Array Literal Stuff                 ***/
    "array-nil" = ArrayNil,
    "array-cons" = ArrayCons([Id; 2]),
    "array-access" = ArrayAccess([Id; 2]),

    /***                  Generic Linked List                 ***/
    "nil" = Nil,
    "cons" = Cons([Id; 2]),

    /***                  Type Annotations                    ***/

    // A `type-annotation` node takes the form:
    //
    //     (type-annotation TYPE INTERFACES SUPERCLASSES)
    //
    // where
    //
    // + TYPE is the literal type (e.g., "int", "java.util.List")
    //
    // + INTERFACES is an alphabetically sorted linked list of interfaces that
    //   this type implements
    //
    // + SUPERCLASSES is an inheritance-ordered sorted linked list of
    //   superclasses that this type transitively extends
    "type-annotation" = TypeAnnotation([Id; 3]),

    "instanceof" = InstanceOf([Id; 2]),
  }
}

pub fn is_const(v1: &'static str) -> impl Fn(&mut EGraph, Id, &Subst) -> bool {
    let v1: egg::Var = v1.parse().unwrap();
    move |egraph, _, subst| egraph[subst[v1]].data.constant.is_some()
}

pub fn is_not_const(v1: &'static str) -> impl Fn(&mut EGraph, Id, &Subst) -> bool {
    let v1: egg::Var = v1.parse().unwrap();
    move |egraph, _, subst| !egraph[subst[v1]].data.constant.is_some()
}

pub fn is_var(v1: &'static str) -> impl Fn(&mut EGraph, Id, &Subst) -> bool {
    let v1: egg::Var = v1.parse().unwrap();
    move |egraph, _, subst| egraph[subst[v1]].data.variable.is_some()
}

pub fn are_bit_disjoint_constants(a: &'static str, b: &'static str) 
    -> impl Fn(&mut EGraph, Id, &Subst) -> bool
{
    let a: egg::Var = a.parse().unwrap();
    let b: egg::Var = b.parse().unwrap();
    let zero_int = Peg::Num(JavaInt::from(0));
    let zero_long = Peg::Long(JavaLong::from(0));
    move |egraph, _, subst | {
        let a_const = egraph[subst[a]].data.constant.clone();
        let b_const = egraph[subst[b]].data.constant.clone();
        if a_const.is_some() && b_const.is_some() {
            let a = egraph.add(a_const.unwrap());
            let b = egraph.add(b_const.unwrap());
            let bw_and: Id = egraph.add(Peg::BinAnd([a,b]));
            let nodes = egraph[bw_and].nodes.clone();
            nodes.contains(&zero_int) || nodes.contains(&zero_long)

        }
        else {
            false
        }
    }
}

pub fn is_not_same_var(
    v1: &'static str,
    v2: &'static str,
) -> impl Fn(&mut EGraph, Id, &Subst) -> bool {
    let v1: egg::Var = v1.parse().unwrap();
    let v2: egg::Var = v2.parse().unwrap();
    move |egraph: &mut EGraph, _, subst: &Subst| egraph.find(subst[v1]) != egraph.find(subst[v2])
}

impl Peg {
    pub fn as_bool(&self) -> Option<bool> {
        match self {
            Peg::Bool(b) => Some(*b),
            _ => None,
        }
    }

    pub fn is_const(&self) -> bool {
        match self {
            Peg::Num(_) | Peg::Bool(_) | Peg::Long(_) => true,
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
        match self {
            Peg::Add(..) | Peg::Mul(..) | Peg::Div(..) | Peg::Sub(..)
                         | Peg::BinOr(..) | Peg::BinAnd(..) | Peg::LShift(..)
                         | Peg::URShift(..) | Peg::SRShift(..)
              => true,
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

    pub fn plus(a: &Peg, b: &Peg) -> Option<Peg> {
        match (a, b) {
            (Peg::Num(a), Peg::Num(b)) => Some(Peg::Num(*a + *b)),
            (Peg::Long(a), Peg::Long(b)) => Some(Peg::Long(*a + *b)),
            (Peg::Num(a), Peg::Long(b)) => Some(Peg::Long(*a + *b)),
            (Peg::Long(a), Peg::Num(b)) => Some(Peg::Long(*a + *b)),
            _ => None,
        }
    }

    pub fn equal(a: &Peg, b: &Peg) -> Option<Peg> {
        match (a, b) {
            (Peg::Num(a), Peg::Num(b)) => Some(Peg::Bool(a == b)),
            (Peg::Long(a), Peg::Long(b)) => Some(Peg::Bool(a == b)),
            (Peg::Num(a), Peg::Long(b)) => Some(Peg::Bool(a == b)),
            (Peg::Long(a), Peg::Num(b)) => Some(Peg::Bool(a == b)),
            (Peg::Bool(a), Peg::Bool(b)) => Some(Peg::Bool(a == b)),
            _ => None,
        }
    }

    pub fn nequal(a: &Peg, b: &Peg) -> Option<Peg> {
        match (a, b) {
            (Peg::Num(a), Peg::Num(b)) => Some(Peg::Bool(a != b)),
            (Peg::Long(a), Peg::Long(b)) => Some(Peg::Bool(a != b)),
            (Peg::Num(a), Peg::Long(b)) => Some(Peg::Bool(a != b)),
            (Peg::Long(a), Peg::Num(b)) => Some(Peg::Bool(a != b)),
            (Peg::Bool(a), Peg::Bool(b)) => Some(Peg::Bool(a != b)),
            _ => None,
        }
    }

    pub fn lt(a: &Peg, b: &Peg) -> Option<Peg> {
        match (a, b) {
            (Peg::Num(a), Peg::Num(b)) => Some(Peg::Bool(a.lt(b))),
            (Peg::Long(a), Peg::Long(b)) => Some(Peg::Bool(a.lt(b))),
            (Peg::Num(a), Peg::Long(b)) => Some(Peg::Bool(a.promote_to_java_long().lt(b))),
            (Peg::Long(a), Peg::Num(b)) => Some(Peg::Bool(a.lt(&b.promote_to_java_long()))),
            _ => None,
        }
    }

    pub fn le(a: &Peg, b: &Peg) -> Option<Peg> {
        match (a, b) {
            (Peg::Num(a), Peg::Num(b)) => Some(Peg::Bool(a.le(b))),
            (Peg::Long(a), Peg::Long(b)) => Some(Peg::Bool(a.le(b))),
            (Peg::Num(a), Peg::Long(b)) => Some(Peg::Bool(a.promote_to_java_long().le(b))),
            (Peg::Long(a), Peg::Num(b)) => Some(Peg::Bool(a.le(&b.promote_to_java_long()))),
            _ => None,
        }
    }

    pub fn gt(a: &Peg, b: &Peg) -> Option<Peg> {
        match (a, b) {
            (Peg::Num(a), Peg::Num(b)) => Some(Peg::Bool(a.gt(b))),
            (Peg::Long(a), Peg::Long(b)) => Some(Peg::Bool(a.gt(b))),
            (Peg::Num(a), Peg::Long(b)) => Some(Peg::Bool(a.promote_to_java_long().gt(b))),
            (Peg::Long(a), Peg::Num(b)) => Some(Peg::Bool(a.gt(&b.promote_to_java_long()))),
            _ => None,
        }
    }

    pub fn ge(a: &Peg, b: &Peg) -> Option<Peg> {
        match (a, b) {
            (Peg::Num(a), Peg::Num(b)) => Some(Peg::Bool(a.ge(b))),
            (Peg::Long(a), Peg::Long(b)) => Some(Peg::Bool(a.ge(b))),
            (Peg::Num(a), Peg::Long(b)) => Some(Peg::Bool(a.promote_to_java_long().ge(b))),
            (Peg::Long(a), Peg::Num(b)) => Some(Peg::Bool(a.ge(&b.promote_to_java_long()))),
            _ => None,
        }
    }

    pub fn minus(a: &Peg, b: &Peg) -> Option<Peg> {
        match (a, b) {
            (Peg::Num(a), Peg::Num(b)) => Some(Peg::Num(*a - *b)),
            (Peg::Long(a), Peg::Long(b)) => Some(Peg::Long(*a - *b)),
            (Peg::Num(a), Peg::Long(b)) => Some(Peg::Long(*a - *b)),
            (Peg::Long(a), Peg::Num(b)) => Some(Peg::Long(*a - *b)),
            _ => None,
        }
    }

    pub fn mult(a: &Peg, b: &Peg) -> Option<Peg> {
        match (a, b) {
            (Peg::Num(a), Peg::Num(b)) => Some(Peg::Num(*a * *b)),
            (Peg::Long(a), Peg::Long(b)) => Some(Peg::Long(*a * *b)),
            (Peg::Num(a), Peg::Long(b)) => Some(Peg::Long(*a * *b)),
            (Peg::Long(a), Peg::Num(b)) => Some(Peg::Long(*a * *b)),
            _ => None,
        }
    }

    pub fn div(a: &Peg, b: &Peg) -> Option<Peg> {
        match b {
            Peg::Num(b) => if b.is_zero() { return Some(Peg::Error)},
            Peg::Long(b) => if b.is_zero() { return Some(Peg::Error)},
            _ => return None,
        }
        match (a, b) {
            (Peg::Num(a), Peg::Num(b)) => Some(Peg::Num(*a / *b)),
            (Peg::Long(a), Peg::Long(b)) => Some(Peg::Long(*a / *b)),
            (Peg::Num(a), Peg::Long(b)) => Some(Peg::Long(*a / *b)),
            (Peg::Long(a), Peg::Num(b)) => Some(Peg::Long(*a / *b)),
            _ => None,
        }
    }

    pub fn bin_and(a: &Peg, b: &Peg) -> Option<Peg> {
        match (a, b) {
            (Peg::Num(a), Peg::Num(b)) => Some(Peg::Num(*a & *b)),
            (Peg::Long(a), Peg::Long(b)) => Some(Peg::Long(*a & *b)),
            (Peg::Num(a), Peg::Long(b)) => Some(Peg::Long(*a & *b)),
            (Peg::Long(a), Peg::Num(b)) => Some(Peg::Long(*a & *b)),
            _ => None,
        }
    }

    pub fn bin_or(a: &Peg, b: &Peg) -> Option<Peg> {
        match (a, b) {
            (Peg::Num(a), Peg::Num(b)) => Some(Peg::Num(*a | *b)),
            (Peg::Long(a), Peg::Long(b)) => Some(Peg::Long(*a | *b)),
            (Peg::Num(a), Peg::Long(b)) => Some(Peg::Long(*a | *b)),
            (Peg::Long(a), Peg::Num(b)) => Some(Peg::Long(*a | *b)),
            _ => None,
        }
    }

    pub fn xor(a: &Peg, b: &Peg) -> Option<Peg> {
        match (a, b) {
            (Peg::Num(a), Peg::Num(b)) => Some(Peg::Num(*a ^ *b)),
            (Peg::Long(a), Peg::Long(b)) => Some(Peg::Long(*a ^ *b)),
            (Peg::Num(a), Peg::Long(b)) => Some(Peg::Long(*a ^ *b)),
            (Peg::Long(a), Peg::Num(b)) => Some(Peg::Long(*a ^ *b)),
            _ => None,
        }
    }

    pub fn srshift(a: &Peg, b: &Peg) -> Option<Peg> {
        match (a, b) {
            (Peg::Num(a), Peg::Num(b)) => Some(Peg::Num(*a >> *b)),
            (Peg::Long(a), Peg::Long(b)) => Some(Peg::Long(*a >> *b)),
            (Peg::Num(a), Peg::Long(b)) => Some(Peg::Long(*a >> *b)),
            (Peg::Long(a), Peg::Num(b)) => Some(Peg::Long(*a >> *b)),
            _ => None,
        }
    }

    pub fn urshift(a: &Peg, b: &Peg) -> Option<Peg> {
        match (a, b) {
            _ => None,
        }
    }

    pub fn lshift(a: &Peg, b: &Peg) -> Option<Peg> {
        match (a, b) {
            (Peg::Num(a), Peg::Num(b)) => Some(Peg::Num(*a << *b)),
            (Peg::Long(a), Peg::Long(b)) => Some(Peg::Long(*a << *b)),
            (Peg::Num(a), Peg::Long(b)) => Some(Peg::Long(*a << *b)),
            (Peg::Long(a), Peg::Num(b)) => Some(Peg::Long(*a << *b)),
            _ => None,
        }
    }

    pub fn and(a: &Peg, b: &Peg) -> Option<Peg> {
        match (a, b) {
            (Peg::Bool(a), Peg::Bool(b)) => Some(Peg::Bool(*a && *b)),
            _ => None,
        }
    }

    pub fn or(a: &Peg, b: &Peg) -> Option<Peg> {
        match (a, b) {
            (Peg::Bool(a), Peg::Bool(b)) => Some(Peg::Bool(*a || *b)),
            _ => None,
        }
    }

    pub fn neg(a: &Peg) -> Option<Peg> {
        match a {
            Peg::Num(x) => Some(Peg::Num(- *x)),
            Peg::Long(x) => Some(Peg::Long(- *x)),
            _ => None,
        }
    }
}

/// Data tracked for a PegAnalysis
#[derive(Default, PartialEq, Debug, Clone)]
pub struct PegAnalysisData {
    pub constant: Option<Peg>,
    pub variable: Option<Peg>,
}

impl PegAnalysisData {
    pub fn or(self, a: PegAnalysisData) -> PegAnalysisData {
        PegAnalysisData {
            constant: self.constant.or(a.constant),
            variable: self.variable.or(a.variable),
        }
    }
}

#[derive(Default)]
pub struct PegAnalysis;

fn eval(egraph: &EGraph, enode: &Peg) -> Option<Peg> {
    let x = |i: &Id| egraph[*i].data.constant.clone();

    match enode {
        // Arithmetic
        Peg::Long(_) | Peg::Num(_) | Peg::Bool(_) => Some(enode.clone()),
        Peg::Add([a, b]) => Peg::plus(&x(a)?, &x(b)?),
        Peg::Sub([a, b]) => Peg::minus(&x(a)?, &x(b)?),
        Peg::Mul([a, b]) => Peg::mult(&x(a)?, &x(b)?),
        Peg::Div([a, b]) => Peg::div(&x(a)?, &x(b)?),

        Peg::Neg(a) => Peg::neg(&x(a)?),

        // Binary Ops
        Peg::BinAnd([a, b]) => Peg::bin_and(&x(a)?, &x(b)?),
        Peg::BinOr([a, b]) => Peg::bin_or(&x(a)?, &x(b)?),
        Peg::Xor([a, b]) => Peg::xor(&x(a)?, &x(b)?),

        // Shifts
        Peg::SRShift([a, b]) => Peg::srshift(&x(a)?, &x(b)?),
        Peg::LShift([a, b]) => Peg::lshift(&x(a)?, &x(b)?),

        // Comparison
        Peg::Gte([a, b]) => Peg::ge(&x(a)?, &x(b)?),
        Peg::Gt([a, b]) => Peg::gt(&x(a)?, &x(b)?),
        Peg::Lte([a, b]) => Peg::le(&x(a)?, &x(b)?),
        Peg::Lt([a, b]) => Peg::lt(&x(a)?, &x(b)?),

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

impl Analysis<Peg> for PegAnalysis {
    type Data = PegAnalysisData;
    fn merge(&self, to: &mut Self::Data, from: Self::Data) -> bool {
        merge_if_different(to, to.clone().or(from))
    }

    fn make(egraph: &EGraph, enode: &Peg) -> Self::Data {
        let _x = |i: &Id| egraph[*i].data.clone();
        match enode {
            Peg::Var(_) => PegAnalysisData {
                constant: None,
                variable: Some(enode.clone()),
            },
            _ => PegAnalysisData {
                constant: eval(egraph, enode),
                variable: None,
            },
        }
    }

    fn modify(egraph: &mut EGraph, id: Id) {
        if let Some(c) = egraph[id].data.constant.clone() {
            let const_id = egraph.add(c);
            egraph.union(id, const_id);
        }
    }
}

/// Convert a `RecExpr<Peg>` to an S-expression `String`. For instance,
/// `[Var("a"), Var("b"), Plus([0, 1])]` will be converted to
/// `(+ (var a) (var b))``
///
/// WARNING: This can blow up if there is a lot of node sharing/duplication in
/// the RecExpr. Also, if for whatever reason the RecExpr is malformed (cyclic),
/// this will loop infinitely.
pub fn to_sexp_string(expr: &RecExpr<Peg>, i: usize) -> String {
    let expr_ref = expr.as_ref();
    let node = expr_ref.get(i).unwrap();
    let op = node.display_op().to_string();
    if node.is_leaf() {
        op
    } else {
        let mut vec = vec![op];
        node.for_each(|id| vec.push(to_sexp_string(expr, id.into())));
        format!("({})", vec.join(" "))
    }
}
