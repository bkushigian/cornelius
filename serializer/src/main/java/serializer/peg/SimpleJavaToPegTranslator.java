package serializer.peg;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.type.Type;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Translate a SimpleJava program to Peg
 */
public class SimpleJavaToPegTranslator {

    final SimpleValidator validator = new SimpleValidator();
    final PegStmtVisitor stmtVisitor = new PegStmtVisitor();
    final PegClassVisitor classVisitor = new PegClassVisitor();

    /**
     * Translate a {@code CompilationUnit} into a set of PEGs, one for each method.
     * @param cu the compilation unit to translate
     * @return a map from method names to their representative PEGs
     */
    public Map<String, PegNode> translate(final CompilationUnit cu) {
        final Map<String, PegNode> result = new HashMap<>();
        for (TypeDeclaration<?> type : cu.getTypes()) {
            if (type.isClassOrInterfaceDeclaration()) {
                final ClassOrInterfaceDeclaration ctype = type.asClassOrInterfaceDeclaration();
                if (ctype.isInterface()) continue;
                final PegClassVisitor.ClassVisitorResult classVisitorResult = classVisitor.visit(ctype);
                for (MethodDeclaration method : ctype.getMethods()) {
                    final String methDeclStr = method.getDeclarationAsString(false, false, false);

                    translate(method, classVisitorResult).ifPresent(t -> result.put(methDeclStr.substring(methDeclStr.indexOf(' ') + 1)
                            .replaceAll("\\s+", ""), t));
                }
            }
        }
        return result;
    }

    public Optional<PegNode> translate(final CompilationUnit cu, final String canonical) {
        if (canonical == null) {
            return Optional.empty();
        }

        for (TypeDeclaration<?> type : cu.getTypes()) {
            if (type.isClassOrInterfaceDeclaration()) {
                final ClassOrInterfaceDeclaration ctype = type.asClassOrInterfaceDeclaration();
                final PegClassVisitor.ClassVisitorResult classVisitorResult = classVisitor.visit(ctype);
                if (ctype.isInterface()) continue;
                for (MethodDeclaration method : ctype.getMethods()) {
                    if (canonical.equals(Util.canonicalName(method))) {
                        return translate(method, classVisitorResult);
                    }
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Translate a {@code MethodDeclaration} into a PEG
     * @param n the method decl to be translated
     * @param classVisitorResult results/data obtained by a visit to the owning class obtained by the
     *       {@code * PegClassVisitor}
     * @return {@code Optional.empty} if the method cannot be translated, and {@code Optional.of(peg)}, where
     *     {@code peg} is a PEG node representing the method
     */
    public Optional<PegNode> translate(final MethodDeclaration n,
                                       final PegClassVisitor.ClassVisitorResult classVisitorResult) {
        if (!validator.validate(n)) {
            return Optional.empty();
        }
        if (n.getType().isVoidType()) {
            return Optional.empty();
        }
        final NodeList<Parameter> parameters = n.getParameters();

        final List<String> params =
                parameters.stream().map(NodeWithSimpleName::getNameAsString).collect(Collectors.toList());
        if (!n.isStatic()) {
            params.add(0, "this");
        }

        final PegContext initCtx = PegContext.initWithParams(classVisitorResult.getFieldNames(), params);
        final PegContext ctx = n.accept(stmtVisitor, initCtx);
        return ctx.asPeg();
    }

}
