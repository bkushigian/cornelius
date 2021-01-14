package serializer.peg;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import serializer.peg.testing.TestPairs;

import java.util.*;

public class PegStmtVisitor extends GenericVisitorAdapter<PegContext, PegContext> {
    final PegExprVisitor pev = new PegExprVisitor();
    /**
     * Map methods to all collected TestPairs
     */
    public TestPairs testPairs;

    public PegStmtVisitor(boolean scrapeComments) {
        testPairs = new TestPairs(scrapeComments);
    }

    public TestPairs getTestPairs() {
        return testPairs;
    }

    @Override
    public PegContext visit(MethodDeclaration n, PegContext ctx) {
        final PegContext result = super.visit(n, ctx);
        testPairs.scrape(n, result.exprResult());
        // Test if there was an implicit return node at the end of the method, and if so, if there was another
        // explicit return node in the body somewhere
        if (n.getBody().isPresent()) {
            NodeList<Statement> statements = n.getBody().get().getStatements();
            if (!statements.get(statements.size() - 1).isReturnStmt() && ctx.getReturnNode() != null) {
                throw new RuntimeException("ExplicitAndImplicitReturnNodes");
            }
        }
        return result;
    }

    @Override
    public PegContext visit(AssignExpr n, PegContext ctx) {
        final ExpressionResult er = n.getValue().accept(pev, ctx);
        ctx = er.context;
        final PegNode value = er.peg;
        if (n.getTarget().isNameExpr()) {
            final String nameString = n.getTarget().asNameExpr().getNameAsString();

            if (ctx.isUnshadowedField(nameString)) {
                final FieldAccessExpr fieldAccess = new FieldAccessExpr(new ThisExpr(), nameString);
                return performWrite(fieldAccess, value, ctx);
            }

            return ctx.setLocalVar(n.getTarget().asNameExpr().getNameAsString(), value);
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
        final ExpressionResult er = pev.getPathFromFieldAccessExpr(fieldAccess, ctx);
        ctx = er.context;
        final PegNode target = er.peg;
        return ctx.withHeap(PegNode.wrHeap(target.id, value.id, ctx.heap));
    }

    @Override
    public PegContext visit(BlockStmt n, PegContext ctx) {
        for (Statement s : n.getStatements()) {
            ctx = s.accept(this, ctx);
            if (ctx == null) {
                throw new IllegalStateException("Null context after visit");
            }
        }
        return ctx;
    }

    @Override
    public PegContext visit(ExpressionStmt n, PegContext ctx) {
        PegContext result = n.accept(pev, ctx).context;
        testPairs.scrape(n, result.exprResult());
        return result;
    }

    @Override
    public PegContext visit(SynchronizedStmt n, PegContext arg) {
        throw new RuntimeException("SynchronizedStmt");
    }

    @Override
    public PegContext visit(IfStmt n, PegContext ctx) {
        final ExpressionResult er = n.getCondition().accept(pev, ctx);
        testPairs.scrape(n, er, "cond");
        ctx = er.context;
        final PegNode guard = er.peg;
        final PegContext c1 = n.getThenStmt().accept(this, ctx);
        testPairs.scrape(n, er, "then");
        final PegContext c2 = n.getElseStmt().isPresent() ? n.getElseStmt().get().accept(this, ctx)
                                                          : ctx;
        testPairs.scrape(n, er, "else");
        PegContext combined = PegContext.combine(c1, c2, guard.id);
        testPairs.scrape(n, combined.exprResult());
        return combined;
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
            if (ctx.returnNode != null) {
                throw new RuntimeException("Multiple returns");
            }
            final ExpressionResult er = expr.accept(pev, ctx);
            ctx = er.context;
            ctx.returnNode = er.peg;
        }

        testPairs.scrape(n, ctx.exprResult());
        return ctx;
    }


    @Override
    public PegContext visit(WhileStmt n, PegContext arg) {
        throw new RuntimeException("WhileStmt");
    }

    @Override
    public PegContext visit(DoStmt n, PegContext arg) {
        throw new RuntimeException("DoStmt");
    }

    @Override
    public PegContext visit(ForStmt n, PegContext arg) {
        throw new RuntimeException("ForStmt");
    }

    @Override
    public PegContext visit(BreakStmt n, PegContext arg) {
        throw new RuntimeException("BreakStmt");
    }

    @Override
    public PegContext visit(TryStmt n, PegContext arg) {
        throw new RuntimeException("TryStmt");
    }

    @Override
    public PegContext visit(ThrowStmt n, PegContext arg) {
        throw new RuntimeException("ThrowStmt");
    }

    @Override
    public PegContext visit(YieldStmt n, PegContext arg) {
        throw new RuntimeException("YieldStmt");
    }

    @Override
    public PegContext visit(ExplicitConstructorInvocationStmt n, PegContext arg) {
        throw new RuntimeException("ExplicitConstructorInvocationStmt");
    }

    @Override
    public PegContext visit(LocalClassDeclarationStmt n, PegContext arg) {
        throw new RuntimeException("LocalClassDeclarationStmt");
    }
}
