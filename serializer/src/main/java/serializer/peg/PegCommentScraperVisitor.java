package serializer.peg;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.Map;
import java.util.Optional;

/**
 * Extract an expected peg from javadoc comments.
 */
class PegCommentScraperVisitor extends VoidVisitorAdapter<Map<String, String>> {
  @Override
  public void visit(MethodDeclaration n, Map<String, String> arg) {
    final Optional<Comment> maybeComment = n.getComment();
    if (maybeComment.isPresent()) {
      final Comment comment = maybeComment.get();
      final String content = comment.getContent();
      boolean inPre = false;
      String peg = null;
      for (String s : content.split("\n\\s*\\*")) {
        final String line = s.trim();
        if ("<pre>".equals(line)) {
          inPre = true;
        }
        else if ("</pre>".equals(line)) {
          inPre = false;
        }
        else if (inPre) {
          if (peg != null) {
            throw new RuntimeException("Illformed Comment: multiple peg lines\n:" + peg + "\n" + line);
          }
          peg = line;
          String methodDeclStr = n.getDeclarationAsString(false,false,false).trim();
          methodDeclStr = methodDeclStr.substring(methodDeclStr.indexOf(' ') + 1)
                  .replaceAll("\\s+", "");
          arg.put(methodDeclStr, peg);
        }
      }
    }
  }
}
