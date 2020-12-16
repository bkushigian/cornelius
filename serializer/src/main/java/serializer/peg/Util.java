package serializer.peg;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.nodeTypes.NodeWithParameters;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithAccessModifiers;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithStaticModifier;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

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

  public static String canonicalizeMajorName(String majorSig) {
    return CanonicalNames.fromMajorSig(majorSig);
  }

  public static <T extends NodeWithSimpleName<?> & NodeWithParameters<?>> String canonicalizeDeclarationName(T md) {
    return CanonicalNames.fromDecl(md);
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
     * Return a canonical version of the declared name from a JP ConstructorDeclaration or a MethodDeclaration.
     *
     * @param decl
     * @return
     */
    public static <T extends NodeWithSimpleName<?> & NodeWithParameters<?>> String fromDecl(final T decl) {
      final String methodName = decl instanceof ConstructorDeclaration? "<init>" : decl.getNameAsString();
      final NodeList<Parameter> params = decl.getParameters();
      final List<String> pTypes = new ArrayList<>();
      for (Parameter p : params) {
        Type type = p.getType();
        String entry = type.asString();
        if (type.isClassOrInterfaceType()) {
          ClassOrInterfaceType ct = type.asClassOrInterfaceType();
          StringBuilder sb = new StringBuilder();
          String scope = ct.getScope().map(ClassOrInterfaceType::asString).orElse("");
          sb.append(scope);
          if (!scope.isEmpty()) {
            sb.append('.');
          }
          sb.append(ct.getNameAsString());
          entry = sb.toString();
        }
        if (p.isVarArgs()) {
          entry += "[]";
        }
        pTypes.add(entry);
      }
      String result = String.format("%s(%s)",  methodName, String.join(",", pTypes));
      return result;
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

  public static <T extends NodeWithParameters<?> & NodeWithStaticModifier<?>> List<String> getParameterList(T n) {
    final NodeList<Parameter> parameters = n.getParameters();

    final List<String> params =
            parameters.stream().map(NodeWithSimpleName::getNameAsString).collect(Collectors.toList());
    if (!n.isStatic()) {
      params.add(0, "this");
    }
    return params;
  }

}
