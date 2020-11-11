package serializer.peg.testing;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.Statement;
import serializer.peg.ExpressionResult;

public class TestPair {
  protected final ExpressionResult actual;
  protected final String expected;
  protected final Node node;

  TestPair(ExpressionResult actual, String expected, Node node){
    this.actual = actual;
    this.expected = expected;
    this.node = node;
  }

  public ExpressionResult getActual() {
    return actual;
  }

  public String getExpected() {
    return expected;
  }

  public Node getNode() {
    return node;
  }

  /**
   * An expected/actual test pair for a Stmt
   */
  public static class StmtTestPair extends TestPair {

    public StmtTestPair(ExpressionResult actual, String expected, Statement stmt) {
      super(actual, expected, stmt);
    }

    @Override
    public Statement getNode() {
      return (Statement) node;
    }
  }

  /**
   * An expected/actual test pair for a Stmt
   */
  public static class MethodDeclTestPair extends TestPair {

    public MethodDeclTestPair(ExpressionResult actual, String expected, MethodDeclaration decl) {
      super(actual, expected, decl);
    }

    @Override
    public MethodDeclaration getNode() {
      return (MethodDeclaration) node;
    }
  }
}
