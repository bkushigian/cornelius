package serializer.peg;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Extract an expected peg from javadoc comments.
 */
public class PegCommentScraperVisitor extends VoidVisitorAdapter<Map<String, String>> {

  final String openTag;
  final String closeTag;
  public PegCommentScraperVisitor() {
    this("target-peg");
  }

  public PegCommentScraperVisitor(final String tag) {

    if (tag.contains("<") || tag.contains(">")) {
      throw new IllegalArgumentException("provided tag must not contain angle braces '<' or '>'");
    }
    openTag = "<" + tag + ">";
    closeTag = "</" + tag + ">";
  }

  public static Map<String, String> scrape(final String javaFile, final String tag) throws FileNotFoundException {
    final PegTranslator translator = new PegTranslator();
    final CompilationUnit cu = StaticJavaParser.parse(new File(javaFile));
    final Map<String, String> commentPegs = new HashMap<>();
    cu.accept(new PegCommentScraperVisitor(tag), commentPegs);
    return commentPegs;
  }

  @Override
  public void visit(MethodDeclaration n, Map<String, String> arg) {
    final Optional<Comment> maybeComment = n.getComment();
    if (maybeComment.isPresent()) {
      final Comment comment = maybeComment.get();
      final String content = comment.getContent();
      boolean inTag = false;
      boolean foundTag = false;
      StringBuilder sb = new StringBuilder();

      for (String s : content.split("\n\\s*\\*")) {
        final String line = s.trim();
        if (openTag.equals(line)) {
          if (inTag) {
            throw new IllegalStateException("Encountered nested tags");
          }
          if (foundTag) {
            throw new IllegalStateException("Encountered Multiple tags");
          }

          inTag = true;
          foundTag = true;
        }
        else if (closeTag.equals(line)) {
          if (!inTag) {
            throw new IllegalStateException("Encountered closing tag while not in an open tag");
          }
          inTag = false;
          String methodDeclStr = n.getDeclarationAsString(false,false,false).trim();
          methodDeclStr = methodDeclStr.substring(methodDeclStr.indexOf(' ') + 1)
                  .replaceAll("\\s+", "");
          arg.put(methodDeclStr, sb.toString());
        }
        else if (inTag) {
          sb.append(line);
          sb.append(' ');
        }
      }
    }
  }
}
