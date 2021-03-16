package serializer.peg;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PegExprVisitor extends com.github.javaparser.ast.visitor.GenericVisitorAdapter<ExpressionResult,
        PegContext> {

    @Override
    public ExpressionResult visit(BinaryExpr n, PegContext context) {
        final ExpressionResult lhs = n.getLeft() .accept(this, context);
        ExpressionResult rhs = n.getRight().accept(this, lhs.context);

        if (n.getOperator() == BinaryExpr.Operator.DIVIDE || n.getOperator() == BinaryExpr.Operator.REMAINDER) {
            // If this is a division or a remainder operator, add a check for div-by-zero
            final PegNode denominatorIsZero = PegNode.opNode(PegOp.EQ, rhs.peg.id, PegNode.intLit(0).id);
            PegNode throwCond;
            if (rhs.context.exitConditions.isEmpty()) {
                throwCond = denominatorIsZero;
            } else {
                // (&& haven-not-exited denominator-is-zero)
                final PegNode haveNotExited = PegNode.opNode(PegOp.NOT, PegNode.exitConditions(rhs.context.exitConditions).id);
                throwCond = PegNode.opNode(PegOp.AND, haveNotExited.id, denominatorIsZero.id);
            }
            rhs = rhs.withContext(rhs.context.withExceptionCondition(throwCond, PegNode.exception("java.lang" +
                    ".DivideByZeroError")));
        }

        return handleBinExpr(n, lhs, rhs);
    }

    /**
     * Handle pure binary expressions. This attempts to do constant folding
     * @param n operator
     * @param lhs left expr
     * @param rhs right expr
     * @return PegNode representing this binary expression
     */
    private ExpressionResult handleBinExpr(BinaryExpr n, ExpressionResult lhs, ExpressionResult rhs) {
        final Optional<Integer> li = lhs.peg.asInteger(), ri = rhs.peg.asInteger();
        final Optional<Boolean> lb = lhs.peg.asBoolean(), rb = rhs.peg.asBoolean();

        switch (n.getOperator()) {
            case OR:
            {
                if (lb.isPresent() && rb.isPresent()) {
                    return PegNode.boolLit(lb.get() || rb.get()).exprResult(rhs.context);
                }
                // The phi node should check if the lhs is true.
                //    If it is, return true,
                //    otherwise return the rhs
                final PegNode phi = PegNode.phi(lhs.peg.id, PegNode.boolLit(true).id, rhs.peg.id);
                PegContext combined = PegContext.combine(lhs.context, rhs.context, lhs.peg.id);
                return phi.exprResult(combined);
            }
            case AND:
            {
                if (lb.isPresent() && rb.isPresent()) {
                    return PegNode.boolLit(lb.get() && rb.get()).exprResult(rhs.context);
                }
                final PegNode phi = PegNode.phi(lhs.peg.id, rhs.peg.id, PegNode.boolLit(false).id);
                PegContext combined = PegContext.combine(rhs.context, lhs.context, lhs.peg.id);
                return phi.exprResult(combined);
            }
            case BINARY_OR:
            {
                if (li.isPresent() && ri.isPresent()) {
                    return PegNode.intLit(li.get() | ri.get()).exprResult(rhs.context);
                }
                return PegNode.opNodeFromPegs(PegOp.BIN_OR, lhs.peg, rhs.peg).exprResult(rhs.context);
            }
            case BINARY_AND:
            {
                if (li.isPresent() && ri.isPresent()) {
                    return PegNode.intLit(li.get() & ri.get()).exprResult(rhs.context);
                }
                return PegNode.opNodeFromPegs(PegOp.BIN_AND, lhs.peg, rhs.peg).exprResult(rhs.context);
            }
            case XOR:
            {
                if (li.isPresent() && ri.isPresent()) {
                    return PegNode.intLit(li.get() ^ ri.get()).exprResult(rhs.context);
                }
                if (lb.isPresent() && rb.isPresent()) {
                    return PegNode.boolLit(lb.get() ^ rb.get()).exprResult(rhs.context);
                }
                return PegNode.opNodeFromPegs(PegOp.XOR, lhs.peg, rhs.peg).exprResult(rhs.context);
            }
            case EQUALS:
            {
                if (lhs.equals(rhs)) {
                    return PegNode.boolLit(true).exprResult(rhs.context);
                }
                return PegNode.opNodeFromPegs(PegOp.EQ, lhs.peg, rhs.peg).exprResult(rhs.context);
            }
            case NOT_EQUALS:
            {
                if (lhs.equals(rhs)) {
                    return PegNode.boolLit(false).exprResult(rhs.context);
                }
                return PegNode.opNodeFromPegs(PegOp.NE, lhs.peg, rhs.peg).exprResult(rhs.context);
            }
            case LESS:
                return PegNode.opNodeFromPegs(PegOp.LT, lhs.peg, rhs.peg).exprResult(rhs.context);
            case GREATER:
                return PegNode.opNodeFromPegs(PegOp.GT, lhs.peg, rhs.peg).exprResult(rhs.context);
            case LESS_EQUALS:
                return PegNode.opNodeFromPegs(PegOp.LE, lhs.peg, rhs.peg).exprResult(rhs.context);
            case GREATER_EQUALS:
                return PegNode.opNodeFromPegs(PegOp.GE, lhs.peg, rhs.peg).exprResult(rhs.context);
            case LEFT_SHIFT:
                return PegNode.opNodeFromPegs(PegOp.LSHIFT, lhs.peg, rhs.peg).exprResult(rhs.context);
            case SIGNED_RIGHT_SHIFT:
                return PegNode.opNodeFromPegs(PegOp.SRSHIFT, lhs.peg, rhs.peg).exprResult(rhs.context);
            case UNSIGNED_RIGHT_SHIFT:
                return PegNode.opNodeFromPegs(PegOp.URSHIFT, lhs.peg, rhs.peg).exprResult(rhs.context);
            case PLUS:
            {
                if (li.isPresent() && ri.isPresent()) {
                    return PegNode.intLit(li.get() + ri.get()).exprResult(rhs.context);
                }
                return PegNode.opNodeFromPegs(PegOp.PLUS, lhs.peg, rhs.peg).exprResult(rhs.context);
            }
            case MINUS:
            {
                if (li.isPresent() && ri.isPresent()) {
                    return PegNode.intLit(li.get() - ri.get()).exprResult(rhs.context);
                }
                return PegNode.opNodeFromPegs(PegOp.MINUS, lhs.peg, rhs.peg).exprResult(rhs.context);
            }
            case MULTIPLY:
            {
                if (li.isPresent() && ri.isPresent()) {
                    return PegNode.intLit(li.get() * ri.get()).exprResult(rhs.context);
                }
                return PegNode.opNodeFromPegs(PegOp.TIMES, lhs.peg, rhs.peg).exprResult(rhs.context);
            }
            case DIVIDE:
            {
                if (li.isPresent() && ri.isPresent() ) {
                    if (ri.get() != 0) {
                        return PegNode.intLit(li.get() / ri.get()).exprResult(rhs.context);
                    } else {
                        // This will never be evaluated since we are dividing by zero
                        return PegNode.unit().exprResult(rhs.context);
                    }
                }
                final PegNode cond =  PegNode.opNode(PegOp.EQ, rhs.peg.id, PegNode.intLit(0).id);
                final PegNode value = PegNode.opNode(PegOp.DIVIDE, lhs.peg.id, rhs.peg.id);
                return PegNode.phi(cond.id, PegNode.unit().id, value.id).exprResult(rhs.context);
            }
            case REMAINDER:
            {
                if (li.isPresent() && ri.isPresent()) {
                    if (ri.get() != 0) {
                        return PegNode.intLit(li.get() % ri.get()).exprResult(rhs.context);
                    } else {
                        // This will never be evaluated since we are dividing by zero
                        return PegNode.unit().exprResult(rhs.context);
                    }
                }
                final PegNode cond =  PegNode.opNode(PegOp.EQ, rhs.peg.id, PegNode.intLit(0).id);
                final PegNode value = PegNode.opNode(PegOp.REMAINDER, lhs.peg.id, rhs.peg.id);
                return PegNode.phi(cond.id, PegNode.unit().id, value.id).exprResult(rhs.context);
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
                Optional<Integer> parsed = Util.parseInt(String.format("-%s", i.getValue()));
                if (parsed.isPresent()) return PegNode.intLit(parsed.get()).exprResult(context);
                throw new IllegalStateException("Invalid integer literal: " + i.getValue());
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
            case PREFIX_DECREMENT:
            case POSTFIX_INCREMENT:
            case POSTFIX_DECREMENT:
              return performSideEffectingUnary(n.getExpression(), er, n.getOperator());
            default:
                throw new IllegalStateException("Unrecognized unary operator: " + n.getOperator());
        }
    }

    private ExpressionResult performSideEffectingUnary(final Expression target,
                                                       final ExpressionResult er,
                                                       final UnaryExpr.Operator op)
    {
        String pegOp;
        switch (op) {
        case POSTFIX_DECREMENT:
        case PREFIX_DECREMENT:
            pegOp = "-";
            break;
        case POSTFIX_INCREMENT:
        case PREFIX_INCREMENT:
            pegOp = "+";
            break;
        default:
            throw new IllegalArgumentException("Operator " + op + " is not prefix/postfix-increment/decrement");
        }
        final PegNode value = PegNode.opNode(pegOp, er.peg.id, PegNode.intLit(1).id);
        if (op.isPrefix()) {
            return performAssign(target, value, er.context);
        }
        return performAssign(target, value, er.context).withPeg(er.peg);
    }

    @Override
    public ExpressionResult visit(VariableDeclarationExpr n, PegContext arg) {
        ExpressionResult er = arg.exprResult(PegNode.unit());
        for (VariableDeclarator vd : n.getVariables()) {
            er = vd.accept(this, er.context);
        }
        return er;
    }

    @Override
    public ExpressionResult visit(VariableDeclarator n, PegContext arg) {
        final String name = n.getNameAsString();
        arg = arg.setLocalVar(name, PegNode.unit());
        final Optional<Expression> initializer = n.getInitializer();
        if (initializer.isPresent()) {
            final ExpressionResult er = initializer.get().accept(this, arg);
            arg = er.context.performAssignLocalVar(name, er.peg);

        }
        return arg.exprResult(PegNode.unit());
    }

    @Override
    public ExpressionResult visit(AssignExpr n, PegContext ctx) {
        final ExpressionResult visitValue = n.getValue().accept(this, ctx);
        ctx = visitValue.context;
        final PegNode value = visitValue.peg;
        return performAssign(n.getTarget(), value, ctx);
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

        Optional<Integer> parsed = Util.parseInt(String.format("%s", n.getValue()));
        if (parsed.isPresent()) return PegNode.intLit(parsed.get()).exprResult(context);
        throw new IllegalStateException("Invalid integer literal: " + n.getValue());
    }

    @Override
    public ExpressionResult visit(StringLiteralExpr n, PegContext arg) {
        return PegNode.stringLit(n.getValue()).exprResult(arg);
    }

    @Override
    public ExpressionResult visit(NameExpr n, PegContext context) {
        return context.getLocalVar(n.getNameAsString()).exprResult(context);
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
        return PegNode.opNode(PegOp.ITE, cond.id, thn.id, els.id).exprResult(combined);
    }

    @Override
    public ExpressionResult visit(ThisExpr n, PegContext arg) {
        return arg.getLocalVar("this").exprResult(arg);
    }

    @Override
    public ExpressionResult visit(FieldAccessExpr n, PegContext arg) {
        final ExpressionResult scope = n.getScope().accept(this, arg);
        final PegNode path = PegNode.path(scope.peg.id, n.getNameAsString());
        final PegNode isnull = PegNode.isnull(scope.peg.id);
        final PegNode npe = PegNode.exception("java.lang.NullPointerException");
        final PegContext nullCheck = scope.context.withExceptionCondition(isnull, npe);
        return PegNode.rd(path.id, scope.context.heap.id).exprResult(nullCheck);
    }

    @Override
    public ExpressionResult visit(MethodCallExpr n, final PegContext context) {
        ExpressionResult scope;

        if (n.getScope().isPresent()) {
            scope = n.getScope().get().accept(this, context);
            scope = scope.withExceptionCondition(PegNode.isnull(scope.peg.id),
                    PegNode.exception("java.lang.NullPointerException"));
        } else {
            scope = context.exprResult(context.getLocalVar("this"));
        }

        final List<Integer> actualsPegs = new ArrayList<>();

        // The following variable keeps track of the updated context as we visit arguments
        PegContext ctx = scope.context;

        for (final Expression actual : n.getArguments()) {
            final ExpressionResult er = actual.accept(this, ctx);
            ctx = er.context;
            actualsPegs.add(er.peg.id);
        }
        final PegNode actuals = PegNode.actuals(actualsPegs.toArray(new Integer[]{}));

        final PegNode invocation = PegNode.invoke(ctx.heap.id, scope.peg.id, n.getNameAsString(), actuals.id);
        // We also need to update the context's heap since we've called a method which may have changed heap state
        ctx = context.withHeap(PegNode.projectHeap(invocation.id));
        return PegNode.invokeToPeg(invocation.id).exprResult(ctx);
    }

    @Override
    public ExpressionResult visit(NullLiteralExpr n, PegContext arg) {
        return PegNode.nullLit().exprResult(arg);
    }

    @Override
    public ExpressionResult visit(ObjectCreationExpr n, PegContext arg) {
        final List<Integer> actualsPegs = new ArrayList<>();

        // The following variable keeps track of the updated context as we visit arguments
        PegContext ctx = arg;
        for (final Expression actual : n.getArguments()) {
            final ExpressionResult er = actual.accept(this, ctx);
            ctx = er.context;
            actualsPegs.add(er.peg.id);
        }

        final PegNode actuals = PegNode.actuals(actualsPegs.toArray(new Integer[]{}));
        final PegNode allocation = PegNode.newObject(n.getType().asString(), actuals.id, ctx.heap.id);
        // We also need to update the context's heap since we've called a method which may have changed heap state
        ctx = ctx.withHeap(PegNode.projectHeap(allocation.id));
        return PegNode.invokeToPeg(allocation.id).exprResult(ctx);
    }

    @Override
    public ExpressionResult visit(CastExpr n, PegContext arg) {
        final PegNode typeName = PegNode.typeName(n.getType().toString());
        final ExpressionResult er = n.getExpression().accept(this, arg);

        // Create relevant PegNodes
        final PegNode canCast = PegNode.canCast(er.peg.id, typeName.id);
        final PegNode cast = PegNode.cast(er.peg.id, typeName.id);
        final PegContext ctx = er.context.withExceptionCondition(canCast, PegNode.exception("java.lang.ClassCastException"));
        return ctx.exprResult(cast);
    }

    @Override
    public ExpressionResult visit(ClassExpr n, PegContext arg) {
        throw new RuntimeException("ClassExpr");
    }

    @Override
    public ExpressionResult visit(MethodReferenceExpr n, PegContext arg) {
        throw new RuntimeException("MethodReferenceExpr");
    }

    @Override
    public ExpressionResult visit(LambdaExpr n, PegContext arg) {
        throw new RuntimeException("LambdaExpr");
    }

    @Override
    public ExpressionResult visit(LongLiteralExpr n, PegContext arg) {
        throw new RuntimeException("LongLiteralExpr");
    }

    @Override
    public ExpressionResult visit(InstanceOfExpr n, PegContext arg) {
        throw new RuntimeException("InstanceOf");
    }

    /**
     * Given a target, a value, and a context, create a new Expression result storing that new value in its target.
     * @param target target of assignment
     * @param value value to assign
     * @param ctx context that assignment is happening in
     * @return the {@code ExpressionResult} resulting from the assignment, including {@code value} as the
     *         {@code peg} field.
     */
    private ExpressionResult performAssign(final Expression target, final PegNode value, final PegContext ctx) {
        if (target.isNameExpr()) {
            final String nameString = target.asNameExpr().getNameAsString();
            // Check if this is an implicit dereference (e.g., `x` instead of `this.x`)
            if (ctx.isUnshadowedField(nameString)) {
                // NOTE: We do not need to explicitly use exit conditions: these are already tracked in the heap
                return ctx.performWrite(new FieldAccessExpr(new ThisExpr(), nameString), value, this);
            }
            return value.exprResult(ctx.performAssignLocalVar(nameString, value));
        }
        else if (target.isFieldAccessExpr()) {
            return ctx.performWrite(target.asFieldAccessExpr(), value, this).withPeg(value);
        }
        else if (target.isArrayAccessExpr()) {
            throw new RuntimeException("ArrayAccessExpr");
        }
        else {
            throw new RuntimeException("UnrecognizedAssignmentTarget");
        }
    }
}
