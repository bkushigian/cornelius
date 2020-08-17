package serializer.peg;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;

import java.util.Optional;

public class PegStmtVisitor extends GenericVisitorAdapter<PegContext, PegContext> {
    final PegExprVisitor pev = new PegExprVisitor();

    @Override
    public PegContext visit(MethodDeclaration n, PegContext ctx) {
        return super.visit(n, ctx);
    }

    @Override
    public PegContext visit(AssignExpr n, PegContext ctx) {
        final PegNode value = n.getValue().accept(pev, ctx);
        if (n.getTarget().isNameExpr()) {
            final String nameString = n.getTarget().asNameExpr().getNameAsString();

            if (ctx.isUnshadowedField(nameString)) {
                final FieldAccessExpr fieldAccess = new FieldAccessExpr(new ThisExpr(), nameString);
                return performWrite(fieldAccess, value, ctx);
            }

            return ctx.set(n.getTarget().asNameExpr().getNameAsString(), value);
        }
        else if (n.getTarget().isFieldAccessExpr()) {
            // todo: update heap
            final FieldAccessExpr fieldAccess = n.getTarget().asFieldAccessExpr();
            return performWrite(fieldAccess, value, ctx);
        }
        else {
            throw new RuntimeException("Unrecognized assignment target: " + n.getTarget().toString());
        }
    }

    // Helper function to produce a new Context storing the write
    private PegContext performWrite(FieldAccessExpr fieldAccess, PegNode value, PegContext ctx) {
        final PegNode target = pev.getPathFromFieldAccessExpr(fieldAccess, ctx);
        return ctx.withHeap(PegNode.wr(target.id, value.id, ctx.heap.id));

    }

    @Override
    public PegContext visit(BlockStmt n, PegContext ctx) {
        for (Statement s : n.getStatements()) {
            ctx = s.accept(this, ctx);
        }
        return ctx;
    }

    @Override
    public PegContext visit(ExpressionStmt n, PegContext ctx) {
        if (n.getExpression().isAssignExpr()) {
            return visit((AssignExpr)n.getExpression(), ctx);
        } else if (n.getExpression().isVariableDeclarationExpr()) {
            final VariableDeclarationExpr vde = n.getExpression().asVariableDeclarationExpr();
            for (VariableDeclarator vd : vde.getVariables()) {
                final String name = vd.getNameAsString();
                final Optional<Expression> initializer = vd.getInitializer();
                if (initializer.isPresent()) {
                    final Expression expr = initializer.get();
                    final PegNode node = expr.accept(pev, ctx);
                    ctx = ctx.set(name, node);
                }
            }
            return ctx;
        }
        throw new IllegalStateException("Non-assignment ExpressionStatement: " + n.toString());
    }

    @Override
    public PegContext visit(IfStmt n, PegContext ctx) {
        final PegContext c1 = n.getThenStmt().accept(this, ctx);
        final PegContext c2 = n.getElseStmt().isPresent() ? n.getElseStmt().get().accept(this, ctx)
                                                          : ctx;
        final PegNode guard = n.getCondition().accept(pev, ctx);
        return PegContext.combine(c1, c2, p -> p.fst.equals(p.snd) ? p.fst : PegNode.phi(guard.id, p.fst.id, p.snd.id));
    }

    @Override
    public PegContext visit(EmptyStmt n, PegContext ctx) {
        return ctx;
    }

    @Override
    public PegContext visit(ReturnStmt n, PegContext ctx) {
        // Here we check if we return anything and then update the ctx to track the name of the returned variable.
        // We perform some checks to ensure that everything works correctly, including
        // 1. that the ReturnStmt's expression exists (if not we return an unaltered context
        // 2. that the expression is a NameExpr
        // 3. that the PegContext hasn't already had a return value assigned: there can only be _one_ return statement
        //    per method
        // Items 2 and 3 are redundant for the validator but we sanity check them anyway.

        final Optional<Expression> optExpr = n.getExpression();
        if (optExpr.isPresent()) {
            final Expression expr = optExpr.get();
            if (!expr.isNameExpr()) {
                throw new IllegalStateException();
            }
            final NameExpr retVar = expr.toNameExpr()
                    .orElseThrow(() -> new IllegalStateException( "SIMPLE programs must return a designated RETURN" +
                            " variable but " + expr.toString() + " was found instead"));
            if (ctx.returnVar != null) {
                throw new IllegalStateException("SIMPLE programs cannot return multiple times");
            }
            final PegNode retNode = expr.accept(pev, ctx);
            ctx.returnVar = retVar.getNameAsString();
            ctx.returnNode = retNode;
        }
        return ctx;
    }

}
