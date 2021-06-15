package serializer.peg;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import serializer.peg.testing.TestPairs;
import com.google.common.collect.ImmutableSet;

import java.util.*;
import java.util.stream.Collectors;

public class PegStmtVisitor extends GenericVisitorAdapter<ExpressionResult, PegContext> {
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
    public ExpressionResult visit(MethodDeclaration n, PegContext ctx) {
        final ExpressionResult result = super.visit(n, ctx);
        testPairs.scrape(n, result);
        // Test if there was an implicit return node at the end of the method, and if so, if there was another
        // explicit return node in the body somewhere
        if (n.getBody().isPresent()) {
            NodeList<Statement> statements = n.getBody().get().getStatements();
            if (!statements.get(statements.size() - 1).isReturnStmt() && ctx.getReturnNode() != null) {
                throw new RuntimeException("InvalidReturn");
            }
        }
        return result;
    }

    @Override
    public ExpressionResult visit(AssignExpr n, PegContext ctx) {
        final ExpressionResult er = n.getValue().accept(pev, ctx);
        ctx = er.context;
        final PegNode value = er.peg;
        if (n.getTarget().isNameExpr()) {
            final String nameString = n.getTarget().asNameExpr().getNameAsString();

            if (ctx.isUnshadowedField(nameString)) {
                final FieldAccessExpr fieldAccess = new FieldAccessExpr(new ThisExpr(), nameString);
                return performWrite(fieldAccess, value, ctx).exprResult();
            }

            return ctx.setLocalVar(n.getTarget().asNameExpr().getNameAsString(), value).exprResult();
        }
        else if (n.getTarget().isFieldAccessExpr()) {
            // todo: update heap
            final FieldAccessExpr fieldAccess = n.getTarget().asFieldAccessExpr();
            return performWrite(fieldAccess, value, ctx).exprResult();
        }
        else {
            throw new RuntimeException("Unrecognized assignment target: " + n.getTarget().toString());
        }
    }

    // Helper function to produce a new Context storing the write
    private PegContext performWrite(FieldAccessExpr fieldAccess, PegNode value, PegContext ctx) {
        final ExpressionResult er = ctx.getPathFromFieldAccessExpr(fieldAccess, pev);
        ctx = er.context;
        final PegNode target = er.peg;
        return ctx.withHeap(PegNode.wrHeap(target.id, value.id, ctx.heap));
    }

    @Override
    public ExpressionResult visit(BlockStmt n, PegContext ctx) {
        ExpressionResult er = ctx.exprResult();
        for (Statement s : n.getStatements()) {
            er = s.accept(this, er.context);
            if (er == null || er.context == null) {
                throw new IllegalStateException("Null context after visit");
            }
        }
        return er;
    }

    @Override
    public ExpressionResult visit(ExpressionStmt n, PegContext ctx) {
        final ExpressionResult result = n.accept(pev, ctx).withPeg(PegNode.unit());
        testPairs.scrape(n, result);
        return result;
    }

    @Override
    public ExpressionResult visit(SynchronizedStmt n, PegContext arg) {
        throw new RuntimeException("SynchronizedStmt");
    }

    @Override
    public ExpressionResult visit(IfStmt n, PegContext ctx) {
        final ExpressionResult er = n.getCondition().accept(pev, ctx);
        testPairs.scrape(n, er, "cond");
        final PegNode guard = er.peg;
        final ExpressionResult resultThen = n.getThenStmt().accept(this, er.context);
        testPairs.scrape(n, er, "then");
        final ExpressionResult resultElse = n.getElseStmt().isPresent() ? n.getElseStmt().get().accept(this, er.context) : er;
        testPairs.scrape(n, er, "else");
        ExpressionResult combined = ExpressionResult.combine(guard, resultThen, resultElse);
        testPairs.scrape(n, combined);
        return combined;
    }

    @Override
    public ExpressionResult visit(EmptyStmt n, PegContext ctx) {
        return ctx.exprResult();
    }

    @Override
    public ExpressionResult visit(ReturnStmt n, PegContext ctx) {
        // We sanity check that this return node is the last statement of a method. To do this we get the parent of the
        // parent of this node and ensure it is a MethodDeclaration. Then, we get the statements block of that method
        // and ensure that `n` is the last node in that block.

        // BEGIN SANITY CHECK
        Optional<Node> optNode;
        if ((optNode = n.getParentNode()).isPresent() && (optNode = optNode.get().getParentNode()).isPresent()) {
            if (! (optNode.get() instanceof MethodDeclaration)) {
                throw new RuntimeException("InvalidReturn");
            }
            final MethodDeclaration md = (MethodDeclaration) optNode.get();
            final NodeList<Statement> statements = md.getBody().orElseThrow(IllegalStateException::new).getStatements();
            if (statements.get(statements.size() - 1) != n) {
                throw new RuntimeException("InvalidReturn");
            }
        } else {
            throw new RuntimeException("InvalidReturn");
        }
        // END SANITY CHECK

        if (n.getExpression().isPresent()) {
            final ExpressionResult er = n.getExpression().get().accept(pev, ctx);
            ctx = er.context.withReturnNode(er.peg);
        } else {
            ctx = ctx.withReturnNode(PegNode.unit());
        }

        testPairs.scrape(n, ctx.exprResult());
        return ctx.exprResult();
    }


    @Override
    public ExpressionResult visit(WhileStmt n, PegContext ctx) {
        // init theta nodes
        ImmutableSet<String> vars = ctx.localVariableLookup.keySet();
        List<String> sortedVars = vars.stream().filter(s -> !s.equals("this")).sorted().collect(Collectors.toList());
        for (String var: sortedVars) {
            PegNode.ThetaNode theta = PegNode.theta(ctx.getLocalVar(var).id);
            ctx = ctx.setLocalVar(var, theta);
        }
        PegNode initState = PegNode.theta(ctx.heap.state);
        PegNode initStatus = PegNode.theta(ctx.heap.status);
        PegContext initCtx = ctx.withHeap(PegNode.heap(initState.id, initStatus.id));

        // visit cond and apply side effects
        ExpressionResult cond = n.getCondition().accept(pev, initCtx);
        testPairs.scrape(n, cond, "cond");
        ctx = cond.context;

        // visit body and apply side effects
        ExpressionResult body = n.getBody().accept(this, ctx);
        ctx = body.context;

        // theta assignment
        for (String var: sortedVars) {
            PegNode theta = initCtx.getLocalVar(var);
            PegNode.assignTheta(theta.id, ctx.getLocalVar(var).id); 
        }
        PegNode.assignTheta(initState.id, ctx.heap.state);
        PegNode.assignTheta(initStatus.id, ctx.heap.status);

        testPairs.scrape(n, ctx.exprResult(), "body");

        // act as if we visit the condition a final time
        ctx = cond.context;

        // construct eval nodes
        PegNode pass = PegNode.pass(cond.peg.id);
        for (String var: sortedVars) {
            PegNode val = ctx.getLocalVar(var);
            ctx = ctx.setLocalVar(var, PegNode.eval(val.id, pass.id));
        }
        PegNode state = PegNode.eval(ctx.heap.state, pass.id);
        PegNode status = PegNode.eval(ctx.heap.status, pass.id);
        ctx = ctx.withHeap(PegNode.heap(state.id, status.id));
       
        testPairs.scrape(n, ctx.exprResult());
        return ctx.exprResult();
    }

    @Override
    public ExpressionResult visit(DoStmt n, PegContext ctx) {
        // visit body once
        ExpressionResult er = n.getBody().accept(this, ctx);
        testPairs.scrape(n, er, "initbody");
        ctx = er.context;

        // init theta nodes
        ImmutableSet<String> vars = ctx.localVariableLookup.keySet();
        List<String> sortedVars = vars.stream().filter(s -> !s.equals("this")).sorted().collect(Collectors.toList());
        for (String var: sortedVars) {
            PegNode.ThetaNode theta = PegNode.theta(ctx.getLocalVar(var).id);
            ctx = ctx.setLocalVar(var, theta);
        }
        PegNode initState = PegNode.theta(ctx.heap.state);
        PegNode initStatus = PegNode.theta(ctx.heap.status);
        PegContext initCtx = ctx.withHeap(PegNode.heap(initState.id, initStatus.id));

        // visit cond and apply side effects
        ExpressionResult cond = n.getCondition().accept(pev, initCtx);
        testPairs.scrape(n, cond, "cond");
        ctx = cond.context;

        // visit body and apply side effects
        ExpressionResult body = n.getBody().accept(this, ctx);
        ctx = body.context;

        // theta assignment
        for (String var: sortedVars) {
            PegNode theta = initCtx.getLocalVar(var);
            PegNode.assignTheta(theta.id, ctx.getLocalVar(var).id); 
        }
        PegNode.assignTheta(initState.id, ctx.heap.state);
        PegNode.assignTheta(initStatus.id, ctx.heap.status);

        testPairs.scrape(n, ctx.exprResult(), "loopbody");

        // act as if we visit the condition a final time
        ctx = cond.context;

        // construct eval nodes
        PegNode pass = PegNode.pass(cond.peg.id);
        for (String var: sortedVars) {
            PegNode val = ctx.getLocalVar(var);
            ctx = ctx.setLocalVar(var, PegNode.eval(val.id, pass.id));
        }
        PegNode state = PegNode.eval(ctx.heap.state, pass.id);
        PegNode status = PegNode.eval(ctx.heap.status, pass.id);
        ctx = ctx.withHeap(PegNode.heap(state.id, status.id));
       
        testPairs.scrape(n, ctx.exprResult());
        return ctx.exprResult();
    }

    @Override
    public ExpressionResult visit(ForStmt n, PegContext ctx) {
        // apply init expressions
        for (Expression expr: n.getInitialization()) {
            ctx = expr.accept(pev, ctx).context;
        }
        testPairs.scrape(n, ctx.exprResult(), "init");

        // init theta nodes
        ImmutableSet<String> vars = ctx.localVariableLookup.keySet();
        List<String> sortedVars = vars.stream().filter(s -> !s.equals("this")).sorted().collect(Collectors.toList());
        for (String var: sortedVars) {
            PegNode.ThetaNode theta = PegNode.theta(ctx.getLocalVar(var).id);
            ctx = ctx.setLocalVar(var, theta);
        }
        PegNode initState = PegNode.theta(ctx.heap.state);
        PegNode initStatus = PegNode.theta(ctx.heap.status);
        PegContext initCtx = ctx.withHeap(PegNode.heap(initState.id, initStatus.id));

        // visit cond and apply side effects
        Expression condExpr = n.getCompare().orElseGet(() -> new BooleanLiteralExpr(true));
        ExpressionResult cond = condExpr.accept(pev, initCtx);
        testPairs.scrape(n, cond, "cond");
        ctx = cond.context;

        // visit body and apply side effects
        ExpressionResult body = n.getBody().accept(this, ctx);
        ctx = body.context;

        // apply update expressions
        for (Expression expr: n.getUpdate()) {
            ctx = expr.accept(pev, ctx).context;
        }

        // theta assignment
        for (String var: sortedVars) {
            PegNode theta = initCtx.getLocalVar(var);
            PegNode.assignTheta(theta.id, ctx.getLocalVar(var).id); 
        }
        PegNode.assignTheta(initState.id, ctx.heap.state);
        PegNode.assignTheta(initStatus.id, ctx.heap.status);

        testPairs.scrape(n, ctx.exprResult(), "body");

        // act as if we visit the condition a final time
        ctx = cond.context;

        // construct eval nodes
        PegNode pass = PegNode.pass(cond.peg.id);
        for (String var: sortedVars) {
            PegNode val = ctx.getLocalVar(var);
            ctx = ctx.setLocalVar(var, PegNode.eval(val.id, pass.id));
        }
        PegNode state = PegNode.eval(ctx.heap.state, pass.id);
        PegNode status = PegNode.eval(ctx.heap.status, pass.id);
        ctx = ctx.withHeap(PegNode.heap(state.id, status.id));
       
        testPairs.scrape(n, ctx.exprResult());
        return ctx.exprResult();
    }

    @Override
    public ExpressionResult visit(ForEachStmt n, PegContext arg) {
        throw new RuntimeException("ForEachStmt");
    }

    @Override
    public ExpressionResult visit(BreakStmt n, PegContext arg) {
        throw new RuntimeException("BreakStmt");
    }

    @Override
    public ExpressionResult visit(TryStmt n, PegContext arg) {
        throw new RuntimeException("TryStmt");
    }

    @Override
    public ExpressionResult visit(ThrowStmt n, PegContext arg) {
        throw new RuntimeException("ThrowStmt");
    }

    @Override
    public ExpressionResult visit(YieldStmt n, PegContext arg) {
        throw new RuntimeException("YieldStmt");
    }

    @Override
    public ExpressionResult visit(ExplicitConstructorInvocationStmt n, PegContext arg) {
        throw new RuntimeException("ExplicitConstructorInvocationStmt");
    }

    @Override
    public ExpressionResult visit(LocalClassDeclarationStmt n, PegContext arg) {
        throw new RuntimeException("LocalClassDeclarationStmt");
    }

    @Override
    public ExpressionResult visit(SwitchStmt n, PegContext arg) {
        throw new RuntimeException("SwitchStmt");
    }
}
