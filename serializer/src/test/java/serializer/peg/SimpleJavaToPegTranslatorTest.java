package serializer.peg;

import org.junit.Test;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class SimpleJavaToPegTranslatorTest {
  @Test
  public void testFieldAccess() throws FileNotFoundException {
    testJavaFile("tests/subjects/field-access/FieldAccess.java");
  }

  @Test
  public void testFieldWrite() throws FileNotFoundException {
    testJavaFile("tests/subjects/field-write/FieldWrite.java");
  }

  @Test
  public void testHeapyExpressions() throws FileNotFoundException {
    testJavaFile("tests/subjects/heapy-expressions/Expressions.java");
  }


  /**
   * Find all methods in a java file with the {@code <pre>(expected (peg form))</pre>} and check the expected PEG
   * against the translated version of the method.
   * @param javaFile
   * @throws FileNotFoundException
   */
  public void testJavaFile(final String javaFile) throws FileNotFoundException {
    final SimpleJavaToPegTranslator translator = new SimpleJavaToPegTranslator();
    final CompilationUnit cu = StaticJavaParser.parse(new File(javaFile));
    final Map<String, String> commentPegs = new HashMap<>();
    cu.accept(new PegCommentScraperVisitor(), commentPegs);

    Map<String, PegNode> translated = translator.translate(cu);
    List<String> failed = new ArrayList<>();
    int i = 0;
    for (String m : translated.keySet()) {
      ++i;
      System.out.printf("%-40s [%d/%d]", m, i, translated.size());
      if (commentPegs.containsKey(m)) {
        System.out.print("   ... Found expected PEG ... ");
        final String expectedPeg = commentPegs.get(m);
        final String actualPeg = translated.get(m).toDerefString();

        //assertEquals(String.format("Method %s", m), expectedPeg, actualPeg);
        if (expectedPeg == null) {
          System.out.println("   ... expectedPeg is NULL!!");
          failed.add(m);
        } else if (actualPeg == null) {
          System.out.println("   ... actualPeg is NULL!!");
          failed.add(m);
        } else if (!actualPeg.equals(expectedPeg)) {
          System.out.println("PEGS NOT EQUAL");
          System.out.printf("    \033[91;1mExpected :\033[0m%s\n", expectedPeg);
          System.out.printf("    \033[91;1mActual   :\033[0m%s\n", actualPeg);
          failed.add(m);
        } else {
          System.out.println("PASS");
        }
      } else {
        System.out.println("   ... No expected PEG found");
      }
    }
    // System.out.println(translated.keySet());
    // System.out.println(commentPegs.keySet());
    // assertEquals(0, failed.size());
  }

}