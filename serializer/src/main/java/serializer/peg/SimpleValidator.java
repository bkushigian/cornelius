package serializer.peg;

import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.modules.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Validate that the Java program is in fact a Simple Java program. This traverses an
 * AST via the Vistior pattern and collects all validated Simple Java methods
 */
public class SimpleValidator extends VoidVisitorAdapter<Set<MethodDeclaration>> {
    // Track valid method declarations
    private final Set<MethodDeclaration> validMethods = new HashSet<>();
    // Map invalid method decls to the reason they are invalid
    private final Map<MethodDeclaration, String> invalidMethods = new HashMap<>();

    public SimpleValidator() {
        super();
    }

    public boolean validate(MethodDeclaration n) {
        n.accept(this, validMethods);
        return validMethods.contains(n);
    }

    int returns;

    @Override
    public void visit(MethodDeclaration n, Set<MethodDeclaration> arg) {
        try {
            returns = 0;
            super.visit(n, arg);
            // check return values. For any method with non-void return type we only need to ensure that there
            // is exactly 1 return node. For void, it's a bit trickier, since there might be an early explicit
            // return and no explicit return at the end of the method.
            if (n.getBody().isPresent()) {
                final NodeList<Statement> body = n.getBody().get().getStatements();
                if (body.isNonEmpty() && !body.get(body.size() - 1).isReturnStmt()) {
                    if (!n.getType().isVoidType()) {
                        throw new SimpleValidationException("non-void method " + n.getNameAsString()
                                + "has no final return statement") ;
                    }
                    returns++;
                }
            }
            arg.add(n);
        } catch (SimpleValidationException e) {
            invalidMethods.put(n, e.getMessage());
            System.err.printf("Invalid SIMPLE method `%s`: reason: \"%s\"\n", n.getDeclarationAsString(false, false,
                    true),
                    e.getMessage());
        }
        returns = 0;
    }

    @Override
    public void visit(ReturnStmt n, Set<MethodDeclaration> arg) {
        ++returns;
        super.visit(n, arg);
    }

    @Override
    public void visit(AnnotationDeclaration n, Set<MethodDeclaration> arg) {
        throw new SimpleValidationException("AnnotationDeclaration is not implemented");
    }

    @Override
    public void visit(AnnotationMemberDeclaration n, Set<MethodDeclaration> arg) {
        throw new SimpleValidationException(n.toString());
    }

    @Override
    public void visit(ArrayAccessExpr n, Set<MethodDeclaration> arg) {
        throw new SimpleValidationException(n.toString());
    }

    @Override
    public void visit(ArrayCreationExpr n, Set<MethodDeclaration> arg) {
        throw new SimpleValidationException(n.toString());
    }

    @Override
    public void visit(ArrayInitializerExpr n, Set<MethodDeclaration> arg) {
        throw new SimpleValidationException(n.toString());
    }

    @Override
    public void visit(AssertStmt n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException(n.toString());
    }

    @Override
    public void visit(AssignExpr n, Set<MethodDeclaration> arg) {
        // We allow AsignExprs, but only as direct children of ExpressionStatements
        // throw new SimpleValidationException("Assign expressions should never be reached:" + n.toString());
        super.visit(n, arg);
    }

    @Override
    public void visit(BinaryExpr n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(BlockComment n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(BlockStmt n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(BooleanLiteralExpr n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(BreakStmt n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException("Simple doesn't support breaks");
    }

    @Override
    public void visit(CastExpr n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException("Simple doesn't support casts");
    }

    @Override
    public void visit(CatchClause n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException("Simple doesn't support catch");
    }

    @Override
    public void visit(CharLiteralExpr n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException("Simple doesn't support Chars");
    }

    @Override
    public void visit(ClassExpr n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException("Simple doesn't support ClassExprs");
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(ClassOrInterfaceType n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(CompilationUnit n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(ConditionalExpr n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(ConstructorDeclaration n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException("Simple doesn't support ConstructorDeclarations");
    }

    @Override
    public void visit(ContinueStmt n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException("Simple doesn't support continues");
    }

    @Override
    public void visit(DoStmt n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException("Simple doesn't support DoStmts");
    }

    @Override
    public void visit(DoubleLiteralExpr n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException("Simple doesn't support DoubleLiteralExprs");
    }

    @Override
    public void visit(EmptyStmt n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(EnclosedExpr n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(EnumConstantDeclaration n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException("Simple doesn't support enums");
    }

    @Override
    public void visit(EnumDeclaration n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException("Simple doesn't support enums");
    }

    @Override
    public void visit(ExplicitConstructorInvocationStmt n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException("Simple doesn't support ExplicitConstructorInvocationStmt");
    }

    @Override
    public void visit(ExpressionStmt n, Set<MethodDeclaration> arg) {
        if (n.getExpression().isAssignExpr() || n.getExpression().isVariableDeclarationExpr()) {
            super.visit(n, arg);
        } else {
            throw new SimpleValidationException("Simple only supports assignment ExpressionStmts: " + n.toString());
        }
    }

    @Override
    public void visit(FieldAccessExpr n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(FieldDeclaration n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(ForEachStmt n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException("Simple doesn't support ForEachStmts");
    }

    @Override
    public void visit(ForStmt n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException("Simple doesn't support ForStmts");
    }

    @Override
    public void visit(IfStmt n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(InitializerDeclaration n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException("Simple doesn't support InitializerDeclarations");
    }

    @Override
    public void visit(InstanceOfExpr n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException("Simple doesn't support InstanceOfExprs");
    }

    @Override
    public void visit(IntegerLiteralExpr n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(JavadocComment n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(LabeledStmt n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException("Simple doesn't support LabeledStmts");
    }

    @Override
    public void visit(LineComment n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(LongLiteralExpr n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException("Simple doesn't support Longs");
    }

    @Override
    public void visit(MarkerAnnotationExpr n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException("MarkerAnnotationExpr");
    }

    @Override
    public void visit(MemberValuePair n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException("MemberValuePair");
    }

    @Override
    public void visit(MethodCallExpr n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException("MethodCallExpr");
    }

    @Override
    public void visit(NameExpr n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(NormalAnnotationExpr n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException("NormalAnnotationExpr");
    }

    @Override
    public void visit(NullLiteralExpr n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException("NullLiteralExpr");
    }

    @Override
    public void visit(ObjectCreationExpr n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException("ObjectCreationExpr");
    }

    @Override
    public void visit(PackageDeclaration n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(Parameter n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(PrimitiveType n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(Name n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(SimpleName n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(ArrayType n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException("ArrayType");
    }

    @Override
    public void visit(ArrayCreationLevel n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException("ArrayCreationLevel");
    }

    @Override
    public void visit(IntersectionType n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException("IntersectionType");
    }

    @Override
    public void visit(UnionType n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException(n.asString());
    }

    @Override
    public void visit(SingleMemberAnnotationExpr n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException(n.toString());
    }

    @Override
    public void visit(StringLiteralExpr n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException(n.asString());
    }

    @Override
    public void visit(SuperExpr n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException(n.toString());
    }

    @Override
    public void visit(SwitchEntry n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException(n.toString());
    }

    @Override
    public void visit(SwitchStmt n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException(n.toString());
    }

    @Override
    public void visit(SynchronizedStmt n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException(n.toString());
    }

    @Override
    public void visit(ThisExpr n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(ThrowStmt n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException(n.toString());
    }

    @Override
    public void visit(TryStmt n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException(n.toString());
    }

    @Override
    public void visit(LocalClassDeclarationStmt n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException(n.toString());
    }

    @Override
    public void visit(TypeParameter n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException(n.toString());
    }

    @Override
    public void visit(UnaryExpr n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(UnknownType n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException(n.toString());
    }

    @Override
    public void visit(VariableDeclarationExpr n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(VariableDeclarator n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(VoidType n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(WhileStmt n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException(n.toString());
    }

    @Override
    public void visit(WildcardType n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException(n.toString());
    }

    @Override
    public void visit(LambdaExpr n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException(n.toString());
    }

    @Override
    public void visit(MethodReferenceExpr n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException(n.toString());
    }

    @Override
    public void visit(TypeExpr n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException(n.toString());
    }

    @Override
    public void visit(NodeList n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(ImportDeclaration n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(ModuleDeclaration n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException(n.toString());
    }

    @Override
    public void visit(ModuleRequiresDirective n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException(n.toString());
    }

    @Override
    public void visit(ModuleExportsDirective n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException(n.toString());
    }

    @Override
    public void visit(ModuleProvidesDirective n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException(n.toString());
    }

    @Override
    public void visit(ModuleUsesDirective n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException(n.toString());
    }

    @Override
    public void visit(ModuleOpensDirective n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException(n.toString());
    }

    @Override
    public void visit(UnparsableStmt n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException(n.toString());
    }

    @Override
    public void visit(ReceiverParameter n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException(n.toString());
    }

    @Override
    public void visit(VarType n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException(n.toString());
    }

    @Override
    public void visit(Modifier n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(SwitchExpr n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(TextBlockLiteralExpr n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException(n.toString());
    }

    @Override
    public void visit(YieldStmt n, Set<MethodDeclaration> arg) {
        super.visit(n, arg);
        throw new SimpleValidationException(n.toString());
    }

    static class SimpleValidationException extends RuntimeException {
        public SimpleValidationException(String msg) {
            super(msg);
        }
        //public SimpleValidationException() { super(); }
    }
}
