package serializer.peg;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import serializer.Util;
import serializer.peg.testing.TestPairs;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Translate a SimpleJava program to Peg
 */
public class PegTranslator {

    final Map<String, Integer> failureReasons = new HashMap<>();
    final PegClassVisitor classVisitor = new PegClassVisitor();
    final PegStmtVisitor stmtVisitor;
    /**
     * For testing infrastructure. This is created by {@code stmtVisitor} and if {@code scrapeComments=true} in this
     * constructor, this stores test cases by inspecting comments in translated source code.
     */
    public final TestPairs testPairs;

    public PegTranslator(boolean scrapeComments) {
        stmtVisitor = new PegStmtVisitor(scrapeComments);
        testPairs = stmtVisitor.getTestPairs();
    }

    public PegTranslator() {
        this(false);
    }

    /**
     * Translate a {@code CompilationUnit} into a set of PEGs, one for each method.
     * @param cu the compilation unit to translate
     * @return a map from method names to their representative PEGs
     */
    public Map<String, PegNode> translate(final CompilationUnit cu) {
        final Map<String, PegNode> result = new HashMap<>();
        final List<TypeDeclaration<?>> types = cu.getTypes();
        types.sort(Comparator.comparing(NodeWithSimpleName::getNameAsString));
        for (TypeDeclaration<?> type : types) {
            if (type.isClassOrInterfaceDeclaration()) {
                final ClassOrInterfaceDeclaration ctype = type.asClassOrInterfaceDeclaration();
                if (ctype.isInterface()) continue;
                final PegClassVisitor.ClassVisitorResult classVisitorResult = classVisitor.visit(ctype);
                final List<MethodDeclaration> sortedMethods = ctype.getMethods()
                        .stream()
                        .sorted(Comparator.comparing(MethodDeclaration::getNameAsString))
                        .collect(Collectors.toList());
                for (MethodDeclaration method : sortedMethods) {
                    final String methDeclStr = Util.CanonicalNames.fromDecl(method, cu, ctype);

                    try {
                        final String key = methDeclStr
                                .substring(methDeclStr.indexOf(' ') + 1)
                                .replaceAll("\\s+", "");
                        result.put(key, translate(method, classVisitorResult));

                    } catch (RuntimeException e) {
                        failureReasons.put(e.getMessage(), failureReasons.getOrDefault(e.getMessage(), 0) + 1);
                    }
                }
            }
        }
        return result;
    }

    public PegNode translate(final CompilationUnit cu, final String canonical) {
        if (canonical == null) return PegNode.unit();

        final NodeList<TypeDeclaration<?>> types = cu.getTypes();
        types.sort(Comparator.comparing(TypeDeclaration::getNameAsString));
        for (TypeDeclaration<?> type : types) {
            if (type.isClassOrInterfaceDeclaration()) {
                final ClassOrInterfaceDeclaration ctype = type.asClassOrInterfaceDeclaration();
                final PegClassVisitor.ClassVisitorResult classVisitorResult = classVisitor.visit(ctype);
                if (ctype.isInterface()) continue;
                for (MethodDeclaration method : ctype.getMethods()) {
                    if (canonical.equals(Util.canonicalizeDeclarationName(method))) {
                        return translate(method, classVisitorResult);
                    }
                }
            }
        }
        return PegNode.unit();
    }

    /**
     * Translate a {@code MethodDeclaration} into a PEG
     * @param n the method decl to be translated
     * @param classVisitorResult results/data obtained by a visit to the owning class obtained by the
     *       {@code * PegClassVisitor}
     * @return {@code Optional.empty} if the method cannot be translated, and {@code Optional.of(peg)}, where
     *     {@code peg} is a PEG node representing the method
     */
    public PegNode translate(final MethodDeclaration n,
                                       final PegClassVisitor.ClassVisitorResult classVisitorResult) {
        final PegContext initCtx = PegContext.initWithParams(classVisitorResult.getFieldNames(),
                Util.getParameterList(n));
        final ExpressionResult result = n.accept(stmtVisitor, initCtx);
        return result.context.asPeg().orElseThrow(IllegalStateException::new);
    }

}
