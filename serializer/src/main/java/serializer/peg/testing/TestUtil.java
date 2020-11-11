package serializer.peg.testing;

import com.github.javaparser.ast.comments.Comment;

import java.util.Optional;

public class TestUtil {
  /**
   * Given a coment, look for a {@code <tag>CONTENT</tag>} and return CONTENT.
   * Remove leading strings of the form "\s*\*\s*" on each line.
   * @param comment the comment to parse
   * @return {@code Optional.of(content)} if tag is successfully found and content is parsed; {@code Optional.empty
   * ()} otherwise.
   */
  public static Optional<String> parseTestingComment(final Optional<Comment> comment) {
    if (!comment.isPresent()) return Optional.empty();
    final String content = comment.get().getContent();
    final String openTag = "<expected>";
    final String closeTag = "</expected>";
    boolean inTag = false;
    final StringBuilder sb = new StringBuilder();

    for (String s : content.split("\n\\s*\\** *")) {
      final String line = s.trim();
      if (openTag.equals(line)) {
        if (inTag) {
          throw new IllegalStateException("Encountered nested tags");
        }
        inTag = true;
      } else if (closeTag.equals(line)) {
        if (!inTag) {
          throw new IllegalStateException("Encountered closing tag while not in an open tag in line: " + line);
        }
        return Optional.of(sb.toString());
      } else if (inTag) {
        sb.append(line);
        sb.append(' ');
      }
    }
    return Optional.empty();
  }


}
