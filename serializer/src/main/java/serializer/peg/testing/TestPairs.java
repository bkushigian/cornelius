package serializer.peg.testing;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.Statement;
import serializer.peg.ExpressionResult;
import serializer.peg.Util;

import java.util.*;

public class TestPairs {
  public final Map<String, List<TestPair>> testPairLookupTable;
  public final Map<String, List<String>> paramNameLookup;
  public final boolean scraping;
  public List<TestPair> worklist;

  public TestPairs(boolean scraping) {
    this.scraping = scraping;
    testPairLookupTable = new HashMap<>();
    paramNameLookup = new HashMap<>();
    worklist = new ArrayList<>();
  }

  /**
   * Given a node, scrape it for comments for testing
   * @param n
   * @param actual
   */
  public void scrape(final Node n, final ExpressionResult actual) {
    scrape(n, actual, "expected");
  }

  public void scrape(final Node n, final ExpressionResult actual, final String tag) {
    if (!scraping) return;
    final Optional<String> content = TestUtil.parseTestingComment(n.getComment(), tag);
    if (content.isPresent()) {
      if (n instanceof Statement) {
        worklist.add(new TestPair.StmtTestPair(actual, content.get(), (Statement)n));
      } else if (n instanceof MethodDeclaration) {
        final MethodDeclaration md = (MethodDeclaration)n;
        worklist.add(new TestPair.MethodDeclTestPair(actual, content.get(), md));
      }
    }
    if (n instanceof MethodDeclaration) {
      final MethodDeclaration md = (MethodDeclaration)n;
      final String name = Util.canonicalizeDeclarationName(md);
      final List<String> params = Util.getParameterList(md);
      paramNameLookup.put(name, params);
      testPairLookupTable.put(name, worklist);
      worklist = new ArrayList<>();
    }
  }
}
