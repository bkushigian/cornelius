package serializer.peg;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.HashSet;
import java.util.Set;

/**
 * A visitor to collect class-level information including fields, methods, and constructors.
 * Final fields that are initialized.
 *
 * For now I'm only collecting fields, but eventually I will collect more data as needed.
 *
 * <h2> Tracking Fields</h2>
 * To track fields I currently only care about the field names.
 */
public class PegClassVisitor extends VoidVisitorAdapter<PegClassVisitor.ClassVisitorResult> {
  /**
   * ClassVisitorResult tracks the result of a class visit
   */
  public static class ClassVisitorResult {
    private final Set<String> fieldNames = new HashSet<String>();

    public boolean containsField(String field) {
      return fieldNames.contains(field);
    }

    public Set<String> getFieldNames() {
      return new HashSet<>(fieldNames);
    }
  }

  /**
   * A heper method that creates a ClassVisitorResult and calls into the visitor
   * @param n
   * @return
   */
  public ClassVisitorResult visit(ClassOrInterfaceDeclaration n) {
    ClassVisitorResult result = new ClassVisitorResult();
    visit(n, result);
    return result;
  }

  /**
   * Overridden to not enter methods
   * @param n
   * @param arg
   */
  @Override
  public void visit(MethodDeclaration n, ClassVisitorResult arg) {
    // I'm overriding this because I don't care about anything that happens inside of methods
    // TODO This explicitly ignores nested classes (which we don't handle for now)
  }

  /**
   * Collect declared field names
   * @param n
   * @param arg
   */
  @Override
  public void visit(FieldDeclaration n, ClassVisitorResult arg) {
    for (VariableDeclarator decl : n.getVariables()) {
      arg.fieldNames.add(decl.getNameAsString());
    }
    super.visit(n, arg);
  }
}
