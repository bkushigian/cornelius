package serializer.peg;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;

import java.util.*;
import java.util.stream.Collectors;

public class Util {
  /**
   * Get the class or interface that owns a method declaration.
   * @param md
   * @return the class or interface that owns md
   */
  public static ClassOrInterfaceDeclaration getOwningType(final MethodDeclaration md) {
    Optional<Node> n = md.getParentNode();
    while (n.isPresent()) {
      final Node n_ = n.get();
      if (n_ instanceof ClassOrInterfaceDeclaration) {
        return (ClassOrInterfaceDeclaration) n_;
      }
      n = n_.getParentNode();
    }
    throw new RuntimeException(String.format("MethodDeclaration %s had not ClassOrInterfaceDeclaration ancestor",
            md.getDeclarationAsString(false, false, false)));
  }

  public static String canonicalName(String majorSig) {
    return CanonicalNames.fromMajorSig(majorSig);
  }

  public static String canonicalName(MethodDeclaration md) {
    return CanonicalNames.fromMethodDecl(md);
  }

  /**
   * There are different naming conventions between JP and Major, and this makes hashing a PITA. This utility
   * provides a way to create canonical versions of names.
   *
   * The canonical name will be {@code methodName(type1,type2,type3,...)}, where there are <em>no spaces</em> in the
   * string. Return types are stripped (no {@code int methodName(...)}) since these aren't provided by Major, and no
   * classes, since these are a pain to find in JP (there is a helper method here, but we shouldn't need it).
   */
  public static class CanonicalNames {
    /**
     * Return a canonical version of the method name from a JP MethodDeclaration.
     *
     * @param md
     * @return
     */
    public static String fromMethodDecl(final MethodDeclaration md) {
      final String methodName = md.getNameAsString();
      final NodeList<Parameter> params = md.getParameters();
      final List<String> pTypes =
              params.stream().map(p -> p.getType().asString()).collect(Collectors.toList());
      return String.format("%s(%s)", methodName, String.join(",", pTypes));
    }

    /**
     * Return the canonical version of the name from a Major signature.
     * @param majorSig
     * @return
     */
    public static String fromMajorSig(final String majorSig) {
      final String[] split = majorSig.split("@");
      if (split.length != 2) {
        throw new IllegalArgumentException(String.format("majorSig %s is not a well formed Major signature", majorSig));
      }
      int openIdx = split[1].indexOf('(');
      int closeIdx = split[1].indexOf(')');
      String name = split[1].substring(0, openIdx);
      String rawParams = split[1].substring(openIdx + 1, closeIdx);
      String[] params = rawParams.split(",");
      for (int i = 0; i < params.length; ++i) {
        final int idx = params[i].lastIndexOf('.') + 1;
        params[i] = params[i].substring(idx);
      }
      return String.format("%s(%s)", name, String.join(",", params));
    }
  }

  public static List<String> getParameterList(MethodDeclaration n) {
    final NodeList<Parameter> parameters = n.getParameters();

    final List<String> params =
            parameters.stream().map(NodeWithSimpleName::getNameAsString).collect(Collectors.toList());
    if (!n.isStatic()) {
      params.add(0, "this");
    }
    return params;
  }

}
