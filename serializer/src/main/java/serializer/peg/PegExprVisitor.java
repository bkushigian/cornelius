package serializer.peg;

import com.github.javaparser.ast.expr.*;

import java.util.Optional;

public class PegExprVisitor extends com.github.javaparser.ast.visitor.GenericVisitorAdapter<PegNode, PegContext> {

    @Override
    public PegNode visit(BinaryExpr n, PegContext context) {
        final PegNode lhs = n.getLeft() .accept(this, context);
        final PegNode rhs = n.getRight().accept(this, context);
        final Optional<Integer> li = lhs.asInteger(), ri = rhs.asInteger();
        final Optional<Boolean> lb = lhs.asBoolean(), rb = rhs.asBoolean();
        switch (n.getOperator()) {
            case OR:
            {
                if (lb.isPresent() && rb.isPresent()) {
                    return PegNode.boolLit(lb.get() || rb.get());
                }
                return PegNode.opNodeFromPegs(PegOp.OR, lhs, rhs);
            }
            case AND:
            {
                if (lb.isPresent() && rb.isPresent()) {
                    return PegNode.boolLit(lb.get() && rb.get());
                }
                return PegNode.opNodeFromPegs(PegOp.AND, lhs, rhs);
            }
            case BINARY_OR:
            {
                if (li.isPresent() && ri.isPresent()) {
                    return PegNode.intLit(li.get() | ri.get());
                }
                return PegNode.opNodeFromPegs(PegOp.BIN_OR, lhs, rhs);
            }
            case BINARY_AND:
            {
                if (li.isPresent() && ri.isPresent()) {
                    return PegNode.intLit(li.get() & ri.get());
                }
                return PegNode.opNodeFromPegs(PegOp.BIN_AND, lhs, rhs);
            }
            case XOR:
            {
                if (li.isPresent() && ri.isPresent()) {
                    return PegNode.intLit(li.get() ^ ri.get());
                }
                if (lb.isPresent() && rb.isPresent()) {
                    return PegNode.boolLit(lb.get() ^ rb.get());
                }
                return PegNode.opNodeFromPegs(PegOp.XOR, lhs, rhs);
            }
            case EQUALS:
            {
                if (lhs.equals(rhs)) {
                    return PegNode.boolLit(true);
                }
                return PegNode.opNodeFromPegs(PegOp.EQ, lhs, rhs);
            }
            case NOT_EQUALS:
            {
                if (lhs.equals(rhs)) {
                    return PegNode.boolLit(false);
                }
                return PegNode.opNodeFromPegs(PegOp.NE, lhs, rhs);
            }
            case LESS:
                return PegNode.opNodeFromPegs(PegOp.LT, lhs, rhs);
            case GREATER:
                return PegNode.opNodeFromPegs(PegOp.GT, lhs, rhs);
            case LESS_EQUALS:
                return PegNode.opNodeFromPegs(PegOp.LE, lhs, rhs);
            case GREATER_EQUALS:
                return PegNode.opNodeFromPegs(PegOp.GE, lhs, rhs);
            case LEFT_SHIFT:
                return PegNode.opNodeFromPegs(PegOp.LSHIFT, lhs, rhs);
            case SIGNED_RIGHT_SHIFT:
                return PegNode.opNodeFromPegs(PegOp.SRSHIFT, lhs, rhs);
            case UNSIGNED_RIGHT_SHIFT:
                return PegNode.opNodeFromPegs(PegOp.URSHIFT, lhs, rhs);
            case PLUS:
            {
                if (li.isPresent() && ri.isPresent()) {
                    return PegNode.intLit(li.get() + ri.get());
                }
                return PegNode.opNodeFromPegs(PegOp.PLUS, lhs, rhs);
            }
            case MINUS:
            {
                if (li.isPresent() && ri.isPresent()) {
                    return PegNode.intLit(li.get() - ri.get());
                }
                return PegNode.opNodeFromPegs(PegOp.MINUS, lhs, rhs);
            }
            case MULTIPLY:
            {
                if (li.isPresent() && ri.isPresent()) {
                    return PegNode.intLit(li.get() * ri.get());
                }
                return PegNode.opNodeFromPegs(PegOp.TIMES, lhs, rhs);
            }
            case DIVIDE:
            {
                if (li.isPresent() && ri.isPresent() && ri.get() != 0) {
                    return PegNode.intLit(li.get() / ri.get());
                }
                return PegNode.opNodeFromPegs(PegOp.DIVIDE, lhs, rhs);
            }
            case REMAINDER:
            {
                if (li.isPresent() && ri.isPresent() && ri.get() != 0) {
                    return PegNode.intLit(li.get() % ri.get());
                }
                return PegNode.opNodeFromPegs(PegOp.REMAINDER, lhs, rhs);
            }
            default:
                throw new IllegalStateException("Unrecognized binary operator: " + n.getOperator());
        }
    }

    @Override
    public PegNode visit(UnaryExpr n, PegContext context) {
        // Special case: when Integer.MIN is encountered we will get a parse error by recursively visiting the int
        if (n.getOperator() == UnaryExpr.Operator.MINUS) {
            final Optional<IntegerLiteralExpr> e = n.getExpression().toIntegerLiteralExpr();
            if (e.isPresent()) {
                IntegerLiteralExpr i = e.get();
                return PegNode.intLit(Integer.parseInt(String.format("-%s", i.getValue())));
            }
            return PegNode.opNodeFromPegs(PegOp.UMINUS, n.getExpression().accept(this, context));
        }

        final PegNode peg = n.getExpression().accept(this, context);

        switch (n.getOperator()) {
            case PLUS:
                return peg;
            case LOGICAL_COMPLEMENT: {
                final Optional<Boolean> val = peg.asBoolean();
                if (val.isPresent()) {
                    return PegNode.boolLit(! val.get());
                }
                return PegNode.opNodeFromPegs(PegOp.NOT, peg);
            }
            case BITWISE_COMPLEMENT: {
                final Optional<Integer> val = peg.asInteger();
                if (val.isPresent()) {
                    return PegNode.intLit(~val.get());
                }
                return PegNode.opNodeFromPegs(PegOp.NEG, peg);
            }
            case PREFIX_INCREMENT:
                return PegNode.opNodeFromPegs(PegOp.PREINC, peg);
            case PREFIX_DECREMENT:
                return PegNode.opNodeFromPegs(PegOp.PREDEC, peg);
            case POSTFIX_INCREMENT:
                return PegNode.opNodeFromPegs(PegOp.POSTINC, peg);
            case POSTFIX_DECREMENT:
                return PegNode.opNodeFromPegs(PegOp.POSTDEC, peg);
            default:
                throw new IllegalStateException("Unrecognized unary operator: " + n.getOperator());
        }
    }

    @Override
    public PegNode visit(AssignExpr n, PegContext context) {
        throw new IllegalStateException("Peg doesn't currently handle assignment expressions that are not also " +
                "assignment statements" );
    }

    @Override
    public PegNode visit(BooleanLiteralExpr n, PegContext context) {
        return PegNode.boolLit(n.getValue());
    }

    @Override
    public PegNode visit(EnclosedExpr n, PegContext context) {
        return super.visit(n, context);
    }

    @Override
    public PegNode visit(IntegerLiteralExpr n, PegContext context) {
        return PegNode.intLit(Integer.parseInt(n.getValue()));
    }

    @Override
    public PegNode visit(NameExpr n, PegContext context) {
        return context.get(n.getNameAsString());
    }

    @Override
    public PegNode visit(ConditionalExpr n, PegContext context) {
        final PegNode cond = n.getCondition().accept(this, context),
                      thn  = n.getThenExpr().accept(this, context),
                      els  = n.getElseExpr().accept(this, context);
        final Optional<Boolean> b = cond.asBoolean();
        if (b.isPresent()) {
            return b.get() ? thn : els;
        }
        return PegNode.opNodeFromPegs(PegOp.ITE, cond, thn, els);
    }

    @Override
    public PegNode visit(ThisExpr n, PegContext arg) {
        return arg.get("this");
    }

    @Override
    public PegNode visit(FieldAccessExpr n, PegContext arg) {
        return PegNode.rd(getPathFromFieldAccessExpr(n, arg).id, arg.heap.id);
    }

    public PegNode getPathFromFieldAccessExpr(FieldAccessExpr n, PegContext ctx) {
        // TODO: This only works for field access expressions w/ nothing (like arrays, methods) in the middle.
        // For instance, a.b.c().d.e, or a.b.c[0].d.e will both fail!
        final StringBuilder derefs = new StringBuilder(n.getNameAsString());
        FieldAccessExpr fa = n;

        while (fa.getScope().isFieldAccessExpr()) {
            derefs.insert(0, fa.getName());
            derefs.insert(0, '.');
            fa = fa.getScope().asFieldAccessExpr();
        }
        final PegNode base = fa.getScope().accept(this, ctx);
        return PegNode.path(base.id, derefs.toString());

    }
}
