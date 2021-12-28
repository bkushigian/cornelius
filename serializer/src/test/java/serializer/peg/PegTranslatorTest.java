package serializer.peg;

import org.junit.Test;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import serializer.peg.testing.TestPairs;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class PegTranslatorTest {
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
    final PegTranslator translator = new PegTranslator(true);
    final CompilationUnit cu = StaticJavaParser.parse(new File(javaFile));

    final Map<String, PegNode> translated = translator.translate(cu);
    final TestPairs testPairs = translator.testPairs;
    int i = 0;
    for (String m : translated.keySet()) {
      ++i;
      System.out.printf("%-40s [%d/%d]\n", m, i, translated.size());
      if (testPairs.testPairLookupTable.containsKey(m)) {
        System.out.print("   ... Found expected PEG ... ");
      }
    }
  }
}
