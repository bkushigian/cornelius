package serializer.peg;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class UtilTest {
  static final String JAVA_TEST_FILES;
  static final ClassOrInterfaceDeclaration methodDecls;
  static MethodDeclaration
      // MethodDecls
          add,
          sub,
          incIfTrue,
          isIntString,
          sum,
          concat,
      // CheckGlobalThis
          shouldTraverse,
          visit,
          shouldReportThis,
          getFunctionJsDocInfo
  ;

  // This is taken from the Closure compiler, as distributed by Defects4J, bug id 100, fixed version
  static final ClassOrInterfaceDeclaration checkGlobalThis;

  static {
    JAVA_TEST_FILES = String.format("%s/tests", System.getProperty("user.dir"));
    // Read in MethodDecls
    try {
      final File f = new File(String.format("%s/simple/MethodDecls.java", JAVA_TEST_FILES));
      final CompilationUnit cu = StaticJavaParser.parse(f);
      final Optional<ClassOrInterfaceDeclaration> optMethodDecls = cu.getInterfaceByName("MethodDecls");
      methodDecls = optMethodDecls.orElseThrow(() -> new IllegalStateException(
                      "Could not extract MethodDecls interface from " + f));
      add = methodDecls.getMethodsByName("add").get(0);
      sub = methodDecls.getMethodsByName("sub").get(0);
      incIfTrue = methodDecls.getMethodsByName("incIfTrue").get(0);
      isIntString = methodDecls.getMethodsByName("isIntString").get(0);
      sum = methodDecls.getMethodsByName("sum").get(0);
      concat = methodDecls.getMethodsByName("concat").get(0);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      throw new RuntimeException("Failed to initialize testing when parsing MethodDecls.java file ");
    }


  // Read in CheckGlobalThis
    try {
      final File f = new File(String.format("%s/CheckGlobalThis.java", JAVA_TEST_FILES));
      final CompilationUnit cu = StaticJavaParser.parse(f);
      final Optional<ClassOrInterfaceDeclaration> optMethodDecls = cu.getClassByName("CheckGlobalThis");
      checkGlobalThis = optMethodDecls.orElseThrow(() -> new IllegalStateException(
              "Could not extract CheckGlobalThis interface from " + f));
      shouldTraverse = checkGlobalThis.getMethodsByName("shouldTraverse").get(0);
      visit = checkGlobalThis.getMethodsByName("visit").get(0);
      shouldReportThis = checkGlobalThis.getMethodsByName("shouldReportThis").get(0);
      getFunctionJsDocInfo = checkGlobalThis.getMethodsByName("getFunctionJsDocInfo").get(0);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      throw new RuntimeException("Failed to initialize testing when parsing CheckGlobalThis file");
    }
  }

  @Test
  public void test_fromMethodDecl() {
    assertEquals("add(int,int)", Util.CanonicalNames.fromMethodDecl(add));
    assertEquals("sub(int,int)", Util.CanonicalNames.fromMethodDecl(sub));
    assertEquals("incIfTrue(boolean,int)", Util.CanonicalNames.fromMethodDecl(incIfTrue));
    assertEquals("isIntString(String)", Util.CanonicalNames.fromMethodDecl(isIntString));
    assertEquals("sum(int[])", Util.CanonicalNames.fromMethodDecl(sum));

    assertEquals("shouldTraverse(NodeTraversal,Node,Node)", Util.CanonicalNames.fromMethodDecl(shouldTraverse));
    assertEquals("visit(NodeTraversal,Node,Node)", Util.CanonicalNames.fromMethodDecl(visit));
    assertEquals("shouldReportThis(Node,Node)", Util.CanonicalNames.fromMethodDecl(shouldReportThis));
    assertEquals("getFunctionJsDocInfo(Node)", Util.CanonicalNames.fromMethodDecl(getFunctionJsDocInfo));
  }

  @Test
  public void test_fromMajorSig() {
    assertEquals("max(int,int)", Util.CanonicalNames.fromMajorSig("Max@max(int,int)"));
    assertEquals("computesFalse(boolean,boolean)", Util.CanonicalNames.fromMajorSig("Exprs@computesFalse(boolean,boolean)"));

    assertEquals("shouldTraverse(NodeTraversal,Node,Node)", Util.CanonicalNames.fromMajorSig("com.google.javascript.jscomp.CheckGlobalThis@shouldTraverse(com.google.javascript.jscomp.NodeTraversal,com.google.javascript.rhino.Node,com.google.javascript.rhino.Node)"));
    assertEquals("visit(NodeTraversal,Node,Node)", Util.CanonicalNames.fromMajorSig("com.google.javascript.jscomp.CheckGlobalThis@visit(com.google.javascript.jscomp.NodeTraversal,com.google.javascript.rhino.Node,com.google.javascript.rhino.Node)"));
    assertEquals("shouldReportThis(Node,Node)", Util.CanonicalNames.fromMajorSig("com.google.javascript.jscomp.CheckGlobalThis@shouldReportThis(com.google.javascript.rhino.Node,com.google.javascript.rhino.Node)"));
    assertEquals("getFunctionJsDocInfo(Node)", Util.CanonicalNames.fromMajorSig("com.google.javascript.jscomp.CheckGlobalThis@getFunctionJsDocInfo(com.google.javascript.rhino.Node)"));
  }
}