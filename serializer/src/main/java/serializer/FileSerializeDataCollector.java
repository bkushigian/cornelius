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
  final Set<ClassVisitResult> results = new HashSet<>();
  /**
   * map error messages to the number of times they occurred
   */
  final static Map<String, Set<VisitResult<?>>>  failureReasons = new HashMap<>();

  public static void main(String[] args) {
    final List<String> worklist = new ArrayList<>();

    for (final String arg : args ){
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
      }
      else {
        worklist.add(arg);
      }
    }
    FileSerializeDataCollector dc = new FileSerializeDataCollector(worklist);
    dc.run();
  }

  public FileSerializeDataCollector(List<String> worklist) {
    this.worklist = worklist;
  }

  final PegClassVisitor classVisitor = new PegClassVisitor();
  final PegStmtVisitor stmtVisitor = new PegStmtVisitor(false);

  public Set<ClassVisitResult> run() {
    int successes = 0;
    int failures = 0;
    for (final String file : worklist) {
      try {
        CompilationUnit cu = StaticJavaParser.parse(new File(file));
        for (TypeDeclaration<?> type : cu.getTypes()) {
          if (type.isClassOrInterfaceDeclaration()) {
            final ClassOrInterfaceDeclaration ctype = type.asClassOrInterfaceDeclaration();
            final ClassVisitResult cvr = new ClassVisitResult(file);
            // System.out.println("==================");
            // System.out.println(file);
            // System.out.println();
            if (ctype.isInterface()) continue;
            final PegClassVisitor.ClassVisitorResult classVisitorResult = classVisitor.visit(ctype);
            for (MethodDeclaration method : ctype.getMethods()) {
              visitDecl(classVisitorResult, method, cvr);
            }

            for (ConstructorDeclaration constructor : ctype.getConstructors()) {
              visitDecl(classVisitorResult, constructor, cvr);
            }

            // System.out.println("---");

            // System.out.println("Failures:" + cvr.numFailure);
            // System.out.println("Success:" + cvr.numSuccess);
            successes += cvr.numSuccess;
            failures += cvr.numFailure;
          }
        }
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }
    }

    final int total = successes + failures;
    System.out.println(" ============ SUMMARY =============");
    System.out.printf("Total Attempts: %d\n"    , total);
    System.out.printf("Total Success:  %d (%f)\n", successes, 100.0 * successes / total);
    System.out.printf("Total Failures: %d (%f)\n", failures , 100.0 * failures  / total);
    final List<String> keys = new ArrayList<>(failureReasons.keySet());
    System.out.println(" ----------- Failure Counts ------------ ");

    for (String key : keys) {
      final Set<VisitResult<?>> mvrs = failureReasons.getOrDefault(key, new HashSet<>());
      System.out.printf("[[%s]]: %d\n", key, mvrs.size());
      for (final VisitResult<?> mvr : mvrs) {
        Range range = mvr.decl.getRange().get();
        int line_begin = range.begin.line;
        int line_end = range.end.line;
        final String name = mvr.getQualifiedName();
        System.out.printf("    %s:%d-%d\n", name, line_begin, line_end);
      }
    }
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
      if (e instanceof NullPointerException) {
        //e.printStackTrace();
        //System.exit(1);
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
      return new VisitResult<D>(d, peg);
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
