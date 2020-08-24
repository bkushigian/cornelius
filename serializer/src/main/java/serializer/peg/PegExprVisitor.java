package serializer.peg;

import com.github.javaparser.ast.expr.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PegExprVisitor extends com.github.javaparser.ast.visitor.GenericVisitorAdapter<ExpressionResult,
        PegContext> {

    @Override
    public ExpressionResult visit(BinaryExpr n, PegContext context) {
        final ExpressionResult lhsExprResult = n.getLeft() .accept(this, context);
        final PegNode lhs = lhsExprResult.peg;
        context = lhsExprResult.context;   // This context will keep track of the current context

        final ExpressionResult rhsExprResult = n.getRight().accept(this, context);
        final PegNode rhs = rhsExprResult.peg;
        context = rhsExprResult.context;

        return handleBinExpr(n, lhs, rhs).exprResult(context);
    }

    /**
     * Handle pure binary expressions. This attempts to do constant folding
     * @param n operator
     * @param lhs left expr
     * @param rhs right expr
     * @return PegNode representing this binary expression
     */
    PegNode handleBinExpr(BinaryExpr n, PegNode lhs, PegNode rhs) {
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
    public ExpressionResult visit(UnaryExpr n, PegContext context) {
        // Special case: when Integer.MIN is encountered we will get a parse error by recursively visiting the int
        if (n.getOperator() == UnaryExpr.Operator.MINUS) {
            final Optional<IntegerLiteralExpr> e = n.getExpression().toIntegerLiteralExpr();
            if (e.isPresent()) {
                IntegerLiteralExpr i = e.get();
                return PegNode.intLit(Integer.parseInt(String.format("-%s", i.getValue()))).exprResult(context);
            }

            final ExpressionResult er = n.getExpression().accept(this, context);
            return PegNode.opNodeFromPegs(PegOp.UMINUS, er.peg).exprResult(er.context);
        }

        final ExpressionResult er = n.getExpression().accept(this, context);
        final PegNode peg = er.peg;
        context = er.context;

        switch (n.getOperator()) {
            case PLUS:
                return peg.exprResult(context);
            case LOGICAL_COMPLEMENT: {
                final Optional<Boolean> val = peg.asBoolean();
                if (val.isPresent()) {
                    return PegNode.boolLit(! val.get()).exprResult(context);
                }
                return PegNode.opNodeFromPegs(PegOp.NOT, peg).exprResult(context);
            }
            case BITWISE_COMPLEMENT: {
                final Optional<Integer> val = peg.asInteger();
                if (val.isPresent()) {
                    return PegNode.intLit(~val.get()).exprResult(context);
                }
                return PegNode.opNodeFromPegs(PegOp.NEG, peg).exprResult(context);
            }
            case PREFIX_INCREMENT:
                return PegNode.opNodeFromPegs(PegOp.PREINC, peg).exprResult(context);
            case PREFIX_DECREMENT:
                return PegNode.opNodeFromPegs(PegOp.PREDEC, peg).exprResult(context);
            case POSTFIX_INCREMENT:
                return PegNode.opNodeFromPegs(PegOp.POSTINC, peg).exprResult(context);
            case POSTFIX_DECREMENT:
                return PegNode.opNodeFromPegs(PegOp.POSTDEC, peg).exprResult(context);
            default:
                throw new IllegalStateException("Unrecognized unary operator: " + n.getOperator());
        }
    }

    @Override
    public ExpressionResult visit(AssignExpr n, PegContext context) {
        throw new IllegalStateException("Peg doesn't currently handle assignment expressions that are not also " +
                "assignment statements" );
    }

    @Override
    public ExpressionResult visit(BooleanLiteralExpr n, PegContext context) {
        return PegNode.boolLit(n.getValue()).exprResult(context);
    }

    @Override
    public ExpressionResult visit(EnclosedExpr n, PegContext context) {
        return super.visit(n, context);
    }

    @Override
    public ExpressionResult visit(IntegerLiteralExpr n, PegContext context) {
        return PegNode.intLit(Integer.parseInt(n.getValue())).exprResult(context);
    }

    @Override
    public ExpressionResult visit(NameExpr n, PegContext context) {
        return context.get(n.getNameAsString()).exprResult(context);
    }

    @Override
    public ExpressionResult visit(ConditionalExpr n, PegContext context) {
        final ExpressionResult condEr = n.getCondition().accept(this, context);
        final PegNode cond = condEr.peg;
        final ExpressionResult thnEr = n.getThenExpr().accept(this, condEr.context),
                elsEr = n.getElseExpr().accept(this, condEr.context);
        final PegNode thn  = thnEr.peg,
                els  = elsEr.peg;
        final Optional<Boolean> b = cond.asBoolean();
        if (b.isPresent()) {
            return (b.get() ? thn : els).exprResult(b.get() ? thnEr.context : elsEr.context);
        }
        final PegContext combined = PegContext.combine(thnEr.context, elsEr.context, cond.id);
        return PegNode.opNodeFromPegs(PegOp.ITE, cond, thn, els).exprResult(combined);
    }

    @Override
    public ExpressionResult visit(ThisExpr n, PegContext arg) {
        return arg.get("this").exprResult(arg);
    }

    @Override
    public ExpressionResult visit(FieldAccessExpr n, PegContext arg) {
        final ExpressionResult er = getPathFromFieldAccessExpr(n, arg);
        return PegNode.rd(er.peg.id, arg.heap.id).exprResult(arg);
    }

    @Override
    public ExpressionResult visit(MethodCallExpr n, PegContext context) {
        List<Integer> actualsPegs = new ArrayList<>();
        for (final Expression actual : n.getArguments()) {
            final ExpressionResult er = actual.accept(this, context);
            final PegNode peg = er.peg;
            context = er.context;
            actualsPegs.add(peg.id);
        }
        final PegNode actuals = PegNode.actuals(actualsPegs.toArray(new Integer[]{}));

        final PegNode invocation = PegNode.invoke(context.heap.id, context.get("this").id, n.getNameAsString(), actuals.id);
        // We also need to update the context's heap since we've called a method which may have changed heap state
        context = context.withHeap(PegNode.projectHeap(invocation.id));
        return PegNode.projectVal(invocation.id).exprResult(context);
    }

    @Override
    public ExpressionResult visit(NullLiteralExpr n, PegContext arg) {
        return PegNode.nullLit().exprResult(arg);
    }

    public ExpressionResult getPathFromFieldAccessExpr(FieldAccessExpr n, PegContext ctx) {
        // TODO: This only works for field access expressions w/ nothing (like arrays, methods) in the middle.
        // For instance, a.b.c().d.e, or a.b.c[0].d.e will both fail!
        final StringBuilder derefs = new StringBuilder(n.getNameAsString());
        FieldAccessExpr fa = n;

        while (fa.getScope().isFieldAccessExpr()) {
            derefs.insert(0, fa.getName());
            derefs.insert(0, '.');
            fa = fa.getScope().asFieldAccessExpr();
        }
        final ExpressionResult base = fa.getScope().accept(this, ctx);
        return PegNode.path(base.peg.id, derefs.toString()).exprResult(ctx);

    }
}
