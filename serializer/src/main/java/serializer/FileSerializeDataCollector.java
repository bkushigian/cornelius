package serializer;

import com.github.javaparser.Range;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithParameters;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithAbstractModifier;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithStaticModifier;
import serializer.peg.*;

import java.io.*;
import java.util.*;

/**
 * Point this at a collection of files and try to serialize each method
 */
public class FileSerializeDataCollector {
  final List<String> worklist;
  /**
   * When true, when a NullPointerException is encountered the stacktrace is printed and we exit.
   */
  private boolean strictCheckNulls = false;

  /**
   * Should we log all method names? This is used for checking specifics of which methods failed and
   * which succeeded.
   */
  private boolean logAllMethods = false;

  final Set<ClassVisitResult> results = new HashSet<>();
  /**
   * map error messages to the number of times they occurred
   */
  final static Map<String, Set<VisitResult<?>>>  failureReasons = new HashMap<>();

  /**
   * Keep track of all successful serializations
   */
  final static Set<VisitResult<?>> successes = new HashSet<>();

  private void parseArgs(String...args) {
    for (final String arg : args ) {
      if (arg.startsWith("@")) {
        final String argFile = arg.substring(1);
        try {
          BufferedReader in = new BufferedReader(new FileReader(argFile));
          String st;
          while ((st = in.readLine()) != null) {
            if (!st.trim().isEmpty()) worklist.add(st.trim());
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      } else if ("--strict-npe".equals(arg)) {
        strictCheckNulls = true;
      }
      else if ("--log-all-methods".equals(arg)){
        logAllMethods = true;
      }
      else {
        worklist.add(arg);
      }
    }
  }

  public static void main(String[] args) {
    FileSerializeDataCollector dc = new FileSerializeDataCollector(args);
    dc.run();
  }

  public FileSerializeDataCollector(String...args) {
    worklist = new ArrayList<>();
    parseArgs(args);
  }

  final PegClassVisitor classVisitor = new PegClassVisitor();
  final PegStmtVisitor stmtVisitor = new PegStmtVisitor(false);

  public Set<ClassVisitResult> run() {
    int successes = 0;
    int failures = 0;
    Util.ProgressBar bar = new Util.ProgressBar(worklist.size());
    int i = 0;
    for (final String file : worklist) {
      bar.printBar(i++);
      try {
        CompilationUnit cu = StaticJavaParser.parse(new File(file));
        for (TypeDeclaration<?> type : cu.getTypes()) {
          if (type.isClassOrInterfaceDeclaration()) {
            final ClassOrInterfaceDeclaration ctype = type.asClassOrInterfaceDeclaration();
            final ClassVisitResult cvr = new ClassVisitResult(file);
            if (ctype.isInterface()) continue;
            final PegClassVisitor.ClassVisitorResult classVisitorResult = classVisitor.visit(ctype);
            for (MethodDeclaration method : ctype.getMethods()) {
              visitDecl(classVisitorResult, method, cvr);
            }

            for (ConstructorDeclaration constructor : ctype.getConstructors()) {
              visitDecl(classVisitorResult, constructor, cvr);
            }

            successes += cvr.numSuccess;
            failures += cvr.numFailure;
          }
        }
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }
    }
    bar.printBar(i, "\n");

    final List<String> keys = new ArrayList<>(failureReasons.keySet());
    keys.sort(Comparator.comparingInt(o -> failureReasons.get(o).size()).reversed());
    System.out.println(" ----------- Failure Counts ------------ ");

    for (String key : keys) {
      final Set<VisitResult<?>> mvrs = failureReasons.getOrDefault(key, new HashSet<>());
      System.out.printf("[[%s]]: %d\n", key, mvrs.size());

      if (logAllMethods) {
        for (final VisitResult<?> mvr : mvrs) {
          Range range = mvr.decl.getRange().get();
          int line_begin = range.begin.line;
          int line_end = range.end.line;
          final String name = mvr.getQualifiedName();
          System.out.printf("    %s:%d-%d\n", name, line_begin, line_end);
        }

        System.out.println("SUCCESSES");
        for (VisitResult<?> vr : this.successes) {
          System.out.printf("++%s\n", vr.getQualifiedName());
        }
      }
    }

    final int total = successes + failures;
    System.out.println(" ============ SUMMARY =============");
    System.out.printf("Total Attempts: %d\n"    , total);
    System.out.printf("Total Success:  %d (%f)\n", successes, 100.0 * successes / total);
    System.out.printf("Total Failures: %d (%f)\n", failures , 100.0 * failures  / total);
    return null;
  }

  /**
   * Visit a declaration (Method or Constructor)
   * @param classVisitorResult
   * @param decl
   * @param cvr
   * @param <D>
   */
  <D extends Node & NodeWithAbstractModifier<?> & NodeWithParameters<?> & NodeWithStaticModifier<?> & NodeWithSimpleName<?>>
  void visitDecl(PegClassVisitor.ClassVisitorResult classVisitorResult,
                                                             D decl,
                                                             ClassVisitResult cvr)
  {
    if (decl.isAbstract()) return;
    final PegContext initCtx = PegContext.initWithParams(classVisitorResult.getFieldNames(),
            Util.getParameterList(decl));
    try {
      final PegContext ctx = decl.accept(stmtVisitor, initCtx);
      final VisitResult<?> vr = cvr.add(decl, ctx.asPeg().get());
      // System.out.println("[ + ] " + vr);
    } catch (RuntimeException e){
      if (strictCheckNulls && e instanceof NullPointerException) {
        e.printStackTrace();
        System.exit(1);
      }
      final VisitResult<?> vr = cvr.add(decl, e);
      // System.out.println("[ - ] " + vr);
    }

  }

  /**
   * the result of visiting a class
   */
  public static class ClassVisitResult {
    /**
     * Name of the file that's visited
     */
    final String fileName;

    /**
     * Map each method name to the reason it failed...empty() means success
     */
    final List<VisitResult<?>> methodResults = new ArrayList<>();

    int numSuccess = 0;
    int numFailure = 0;

    ClassVisitResult(final String fileName) {
      this.fileName = fileName;
    }

    public <D extends Node & NodeWithSimpleName<?> & NodeWithParameters<?>>  VisitResult<D> add(D md, Throwable t) {
      final VisitResult<D> result = VisitResult.failure(md, t);
      methodResults.add(result);
      numFailure++;
      return result;
    }

    public <D extends Node & NodeWithSimpleName<?> & NodeWithParameters<?>>  VisitResult<D> add(D md, PegNode peg) {
      final VisitResult<D> result = VisitResult.peg(md, peg);
      methodResults.add(result);
      numSuccess++;
      return result;
    }
  }

  static class VisitResult<D extends Node & NodeWithParameters<?> & NodeWithSimpleName<?>> {
    final D decl;
    final protected PegNode peg;
    final protected Throwable failure;

    VisitResult(D decl, PegNode peg) {
      this.decl = decl;
      this.peg = peg;
      failure = null;
    }

    VisitResult(D decl, Throwable failure) {
      this.decl = decl;
      this.failure = failure;
      peg = null;
    }

    public static <D extends Node & NodeWithParameters<?> & NodeWithSimpleName<?>> VisitResult<D> failure(D d, Throwable t) {
      final VisitResult<D> result = new VisitResult<D>(d, t);
      failureReasons.putIfAbsent(t.getMessage(), new HashSet<>());
      failureReasons.get(t.getMessage()).add(result);
      return result;
    }

    public static <D extends Node & NodeWithParameters<?> & NodeWithSimpleName<?>> VisitResult<D>  peg(D d, final PegNode peg) {
      VisitResult<D> result = new VisitResult<D>(d, peg);
      successes.add(result);
      return result;
    }

    String getQualifiedName() {
      ClassOrInterfaceDeclaration clazz =
              (ClassOrInterfaceDeclaration)decl.getParentNode().orElseThrow(IllegalStateException::new);
      return String.format("%s@%s", clazz.getFullyQualifiedName().get(),
              Util.canonicalizeDeclarationName(decl));
    }

    public Optional<Throwable> failure() {
      return Optional.ofNullable(failure);
    }

    public Optional<PegNode> peg() {
      return Optional.ofNullable(peg);
    }

    public boolean isFailure() {
      return failure != null;
    }

    public boolean isPeg() {
      return peg != null;
    }

    @Override
    public String toString() {
      final String sig = getQualifiedName();
      return isPeg() ? sig + "::" + peg :
              sig + "::" + failure.getMessage();
    }
  }
}
