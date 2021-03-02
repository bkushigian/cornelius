package serializer.peg;

import com.github.javaparser.ast.CompilationUnit;
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
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

import javax.swing.text.html.Option;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
   * The canonical name will be {@code pkg.path.ClassName@methodName(type1,type2,type3,...)}, where there are <em>no
   * spaces</em> in the
   * string. Return types are stripped (no {@code int methodName(...)}) since these aren't provided by Major, and no
   * classes, since these are a pain to find in JP (there is a helper method here, but we shouldn't need it).
   *
   */
  public static class CanonicalNames {
    public static
    <T extends NodeWithSimpleName<?> & NodeWithParameters<?>>
    String
    fromDecl(final T decl, final CompilationUnit cu, ClassOrInterfaceDeclaration owner) {
      final StringBuilder sb = new StringBuilder();
      if (cu.getPackageDeclaration().isPresent()) {
        sb.append(cu.getPackageDeclaration().get().getName().asString()).append('.');
      }
      return sb.append(owner.getNameAsString())
              .append('@')
              .append(fromDecl(decl)).toString();
    }
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
        final Type type = p.getType();
        String entry = type.asString();
        if (type.isClassOrInterfaceType()) {
          ClassOrInterfaceType ct = type.asClassOrInterfaceType();
          StringBuilder sb = new StringBuilder();
          // String scope = ct.getScope().map(ClassOrInterfaceType::asString).orElse("");
          // sb.append(scope);
          // if (!scope.isEmpty()) {
          //   sb.append('.');
          // }
          sb.append(ct.getNameAsString());
          entry = sb.toString();
        }
        if (p.isVarArgs()) {
          entry += "[]";
        }
        if (type.isArrayType()) {
          ArrayType at = type.asArrayType();
          entry = at.asString();
          Type componentType = at.getComponentType();
          if (componentType.isClassOrInterfaceType()) {
            ClassOrInterfaceType cType = componentType.asClassOrInterfaceType();
            entry = cType.getName() + "[]";
          }
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
      if (split.length == 1) {
        // class level mutation
        return majorSig;
      } else if (split.length != 2)  {
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

  /**
   * Decode an int string. Handles Decimal, hex and octal formats.
   *
   * TODO handle binary "[+-]?0bn+" formats
   * @param s
   * @return
   */
  public static Optional<Integer> parseInt(String s) {
    if (s == null) return Optional.empty();
    s = s.toLowerCase();
    try {
      return Optional.of(Integer.decode(s));
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }



  /**
   * Recursively visit a directory structure and collect all Java files
   * @param file root of file tree to explore: can be a File or a Directory
   * @param javaFiles set that will be modified to hold all java files. If null, a new set will be created for you.
   * @return The set with collected Java files
   */
  public static Set<File> collectJavaFiles(final File file, final Set<File> javaFiles) {
    if (file == null) {
      throw new IllegalArgumentException("Parameter `file` in `collectJavaFiles` is `null`");
    }
    if (javaFiles == null) {
      return collectJavaFiles(file, new HashSet<>());
    }
    if (!file.isDirectory()) {
      if (file.getName().endsWith(".java")) {
        javaFiles.add(file);
      }
      return javaFiles;
    }
    final File[] files = file.listFiles();
    assert files != null;                  // This must hold since file.isDirectory()
    for (File f : files) {
      collectJavaFiles(f, javaFiles);
    }
    return javaFiles;
  }

  /**
   * Collect all Java files from a {@code mutants/} directory, and return
   * a map from mutant id Strings to the file path.
   * @param mutantsDir root of file tree to explore: can be a File or a Directory
   * @return The a mapping from mutant ids to file paths
   */
  public static Map<String, File> collectMutantFiles(final File mutantsDir) {
    if (mutantsDir == null) {
      throw new IllegalArgumentException("Parameter `file` in `collectJavaFiles` is `null`");
    }
    if (! mutantsDir.isDirectory()) {
      throw new IllegalArgumentException("`mutants` is not a directory: " + mutantsDir);
    }

    System.out.println("Collecting mutant files");
    Map<String, File> result = new HashMap<>();
    final long starttime = System.currentTimeMillis();
    final File[] files = mutantsDir.listFiles();
    assert files != null;                  // This must hold since file.isDirectory()
    ProgressBar bar = new ProgressBar(files.length);
    long i = 0;
    for (File f : files) {
      bar.printBar(i);
      ++i;
      final String id = f.getName();
      final Set<File> mutants = collectJavaFiles(f, null);
      if (mutants.size() != 1) {
        throw new IllegalStateException("Mutant id " + id + " has " + mutants.size() + " mutants, expected 1");
      }
      result.put(f.getName(), mutants.iterator().next());
    }
    bar.printBar(i, "\n");
    final long endtime = System.currentTimeMillis();
    System.out.printf("Collected %d mutant files in %f seconds\n", result.size(), (endtime-starttime)/1000.0);

    return result;
  }

  public static void recursivelyDelete(File file) throws IOException {
    recursivelyDelete(file.toPath());
  }

  public static void recursivelyDelete(Path path) throws IOException {
    if (!path.toFile().exists()) return;
    if (!Files.walk(path)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .map(File::delete).allMatch(x -> x))
      throw new IllegalStateException("Could not delete all files in path " + path);
  }

  public static class ProgressBar {
    public final long totalJobs;
    public final int width;
    public long startTimeMS = -1;
    private int lastBarSize = 0;

    public ProgressBar(long totalJobs) {
      this(totalJobs, 80);
    }

    public ProgressBar(long totalJobs, int width) {
      startTimeMS = System.currentTimeMillis();
      this.totalJobs = totalJobs;
      this.width = width;
    }

    public String getBar(long completedJobs) {
      double percentDone = ((double)completedJobs) / totalJobs;
      final String progressTag = String.format(" (%5.1f%% in %1.1f sec)",
              percentDone * 100,
              (System.currentTimeMillis() - startTimeMS) / 1000.0);
      int maxBarWidth = width - 2 - progressTag.length();
              StringBuilder sb = new StringBuilder("[");
      int currentBarWidth = (int)(maxBarWidth * percentDone);
      for (int i = 0; i < currentBarWidth; ++i) {
        sb.append('#');
      }
      for (int i = 0; i < (maxBarWidth - currentBarWidth); ++i) {
        sb.append(' ');
      }
      sb.append(']');
      sb.append(progressTag);
      final String result = sb.toString();
      lastBarSize = result.length();
      return result;
    }

    public String getBar(long completedJobs, final String end) {
      return getBar(completedJobs) + end;
    }

    /**
     * Clear the last bar by printing a bunch of spaces followed by a '\r'
     */
    public void clearLastBar() {
      final StringBuilder sb = new StringBuilder();
      for (int i = 0; i < lastBarSize; ++i) {
        sb.append(' ');
      }
      sb.append('\r');

      System.err.print(sb.toString());
    }

    public void printBar(long completedJobs, String end) {
      clearLastBar();
      System.err.print(getBar(completedJobs, end));
    }

    public void printBar(long completedJobs) {
      printBar(completedJobs, "\r");
    }
  }
}
