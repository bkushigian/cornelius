package serializer;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.expr.Expression;
import serializer.peg.*;
import serializer.xml.XMLGenerator;

import javax.xml.transform.TransformerException;
import java.io.*;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ExprSerializer {
  int errors = 0;
  int mutantsLogged = 0;
  int maxExprsLogged = 0;
  int skippedMutants = 0;

  /**
   * A list of expr files
   */
  final List<File> exprsFiles = new ArrayList<>();

  /**
   * a list of directories that contain expr files
   */
  final List<String> exprsDirs = new ArrayList<>();
  final PegExprVisitor pev = new PegExprVisitor();

  /**
   * Count the number of times a peg translation error has occurred
   */
  final Map<String, List<String>> pegTranslationErrorLog = new HashMap<>();

  /**
   * Should we print errors to file at the end of the run?
   */
  private boolean logPegTranslationErrors;

  /**
   * Should we produce verbose output?
   */
  private boolean verbose;

  protected void logPegTranslationError(String error, String source) {
    if (error.startsWith("UnknownStatus:")) {
      error = "UnknownIdentStatus";
    }
    pegTranslationErrorLog.computeIfAbsent(error, k -> new ArrayList<>()).add(source);
  }

  public static void main(String[] args) throws IOException {
    ExprSerializer serializer = new ExprSerializer();
    serializer.parseArgs(args);
    serializer.serialize();
  }

  ExprSerializer() {
  }

  void parseArgs(String...args) {
    for (int i = 0; i < args.length; ++i) {
      final String arg = args[i];
      switch (arg) {
        case "-d":
        case "--exprdir":
          exprsDirs.add(args[++i]);
          break;
        case "--log-translation-errors":
        case "-l":
          this.logPegTranslationErrors = true;
          break;
        case "--verbose":
        case "-v":
          this.verbose = true;
          break;
        default:
          exprsFiles.add(new File(arg));
      }
    }
  }

  void serialize() throws IOException {
    // Setup some output directories
    File[] dirs = Util.setUpOutputDirectory("cornelius", "exprs", "exprs/subjects", "exprs/logs");
    final File subjectsDir = dirs[2];
    final File logsDir = dirs[3];

    processExprsDirs();    // Add all files specified as a directory to the exprsFiles list

    for (File file : exprsFiles) {
      translateExprFile(subjectsDir, file);
    }
    writeSerializationSummary();

    writeLogs(logsDir);
  }

  private void translateExprFile(File subjectsDir, File file) throws IOException {
    if (!file.getName().endsWith(".expr")) {
      System.err.println("Bad expr file ending: " + file.getName());
      return;
    }
    PegNode.clear();
    ExprFile ef = new ExprFile(file);
    ef.writeToCorFile(subjectsDir);
  }

  /**
   * Take all registered exprsDirs and add the contents to file
   */
  private void processExprsDirs() {
    for (String dirPath : exprsDirs) {
      final File exprsDir = new File(dirPath);
      if (!exprsDir.isDirectory()) {
        throw new RuntimeException("Invalid expression directory: " + exprsDir);
      }
      final File[] files = exprsDir.listFiles(f -> f.isFile() && f.getName().endsWith(".expr"));
      if (files == null) continue;
      exprsFiles.addAll(Arrays.asList(files));
    }
  }

  private void writeSerializationSummary() {
    System.out.println("Serialized " + maxExprsLogged + " maximal expressions");
    System.out.println("Serialized " + mutantsLogged + " mutant expressions");
    System.out.println("Skipped " + skippedMutants + " mutant expressions (couldn't parse the original expression)");
  }

  private void writeLogs(File logsDir) throws IOException {
    if (logPegTranslationErrors) {

      Path logPath = Paths.get(logsDir.toString(), "peg-translation-errors.log");
      BufferedWriter translationErrorLog = new BufferedWriter(new FileWriter(logPath.toString()));
      final List<Map.Entry<String, List<String>>> errors = new ArrayList<>(pegTranslationErrorLog.entrySet());
      errors.sort(Comparator.comparingInt(e -> e.getValue().size()));
      for (Map.Entry<String, List<String>> error : errors) {
        translationErrorLog.write(error.getKey() + ": " + error.getValue().size());
        translationErrorLog.newLine();
      }

      translationErrorLog.flush();
      translationErrorLog.close();
    }
  }

  /**
   * An ExprFile represents the contents of a .expr file exported by Major.
   * Upon creating of the ExprFile the raw textual contents are translated into
   * a data structure containing the serialized expressions of each maximal expression.
   */
  class ExprFile {
    Map<String, List<MaxExpr>> nsToMaxExprs;
    private final File inFile;

    ExprFile(File file) throws IOException {
      inFile = file;
      nsToMaxExprs = new HashMap<>();
      // Set up a regex to recognize mutant expressions
      final Pattern mutPattern = Pattern.compile("^\\d+:");

      BufferedReader in = Files.newBufferedReader(Paths.get(file.getPath()));

      {
        MaxExpr maxExpr = null;
        String ns = null;

        int i = 0;
        for (String line; (line = in.readLine()) != null; ) {
          ++i;
          line = line.trim();

          // Handle new namespace
          if (line.startsWith("[")) {
            assert line.endsWith("]");
            ns = line.trim().substring(1, line.length() - 1);

            // Handle new max expression
          } else if (line.startsWith("maxExpr:")) {
            assert ns != null;
            assert maxExpr == null;
            int idx = line.indexOf(":");
            try {
              maxExpr = new MaxExpr(ns, line.trim().substring(idx + 1));
              nsToMaxExprs.computeIfAbsent(ns, k -> new ArrayList<>()).add(maxExpr);
            } catch (ParseProblemException e) {
              maxExpr = null;
            }

          } else if (line.startsWith("startPos:")) {
            assert ns != null;
            if (maxExpr == null) continue;
            int idx = line.indexOf(":");
            maxExpr.startPos = line.substring(idx + 1).trim();

            // Handle maxExpr's identMap
          } else if (line.startsWith("identMap:")) {
            assert ns != null;
            if (maxExpr == null) continue;
            int idx = line.indexOf(":");
            final String mapString = line.substring(idx + 1);
            if (mapString.contains(",")){
              for (String keyVal : mapString.split(",")) {
                String[] split = keyVal.split(":");
                maxExpr.identMap.put(split[0].trim(), split[1].trim());
              }
            }
            // Handle mutated expressions
          } else if (mutPattern.matcher(line).find()) {
            if (maxExpr == null) {
              skippedMutants += 1;
              continue;   // Error parsing maxExpr
            }
            int idx = line.indexOf(":");
            maxExpr.addMutant(line.substring(0, idx), line.substring(idx + 1));

            // Handle maxExpr Separators
          } else if (line.startsWith("-----")) {
            maxExpr = null;
          } else if (!line.isEmpty()) {
            throw new RuntimeException("Invalid file line: " + i + ": " + line);
          }
        }
      } // `ns` and `maxExpr` are out of scope

      for (String ns: nsToMaxExprs.keySet()) {
        for (MaxExpr maxExpr : nsToMaxExprs.get(ns)) {
          maxExpr.computePegNodes();
        }
      }
    }

    void writeToCorFile(final File outputDir) throws IOException {
      final String filename = Paths.get(outputDir.getPath(), inFile.getName() + "-cor").toString();
      final XMLGenerator xml = new XMLGenerator();

      for (final String ns : nsToMaxExprs.keySet()) {
        final List<MaxExpr> maxExprs = nsToMaxExprs.get(ns);
        for (MaxExpr expr : maxExprs) {
          if (expr.peg == null) {
            continue;
          }
          final String nsAndPosition = ns + "@" + expr.startPos;
          xml.addSubject(inFile.getName(), nsAndPosition, expr.peg.id);
          for (Mutant m: expr.mutants) {
            xml.addMutant(nsAndPosition, (new Integer( m.m_no)).toString(), m.peg.id);
          }
        }

      }
      if (!xml.hasSubject()) return;
      xml.addIdTable(PegNode.getIdLookup());

      try {
        xml.writeToFile(filename);
      } catch (TransformerException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Represent a maximal expression and all of it's mutants
   */
  class MaxExpr {
    /**
     * Namespace of this maximal expression
     */
    protected String ns;

    /**
     * The java source of this maximal expression
     */
    protected String source;

    /**
     * The computed JavaParser AST for this maximal expression
     */
    protected Expression tree;

    /**
     * The computed PEG node corresponding to this maximal expression
     */
    protected PegNode peg;

    /**
     * All of the mutants generated by mutating this maximal expression.
     */
    protected List<Mutant> mutants;

    /**
     * The initial context of this maximal expression
     */
    protected PegContext initContext;

    /**
     * The resulting context after this maximal expression is 'executed'
     */
    protected PegContext context;

    /**
     * The source start position in the file, this is used to differentiate maximal expressions in a file.
     */
    protected String startPos;

    /**
     * A flag indicating if there was an error translating the Java source to a PegNode
     */
    protected boolean pegTranslationError = false;

    /**
     * A mapping from identifier name to variable type ("LOCAL" or "GLOBAL" or "OTHER")
     */
    private Map<String, String> identMap;

    protected MaxExpr(String ns, String source) {
      this.ns = ns.trim();
      this.source = source.trim();
      mutants = new ArrayList<>();
      try {
        tree = StaticJavaParser.parseExpression(source);
        maxExprsLogged++;
      } catch (com.github.javaparser.ParseProblemException e) {
        System.out.println("(" + (++errors) + ") Problem parsing source: |" + source + "| in namespace " + ns );
        System.out.println(trimParseErrorMessage(e.getMessage()));
        throw e;
      }
      identMap = new HashMap<>();
    }

    void addMutant(String m_no, String mutExpr) {
      mutants.add(new Mutant(this, m_no, mutExpr));
    }

    /**
     * Use the contents of identMap to create an initial PegContext
     * @return
     */
    private PegContext getInitCtx() {
      final Set<String> globals = new HashSet<>();
      final List<String> locals = new ArrayList<>();
      for (Map.Entry<String, String> f : identMap.entrySet()) {
        switch (f.getValue()) {
          case "LOCAL":
            locals.add(f.getKey());
            break;
          case "GLOBAL":
            globals.add(f.getKey());
            break;
          default:
            throw new RuntimeException("UnknownStatus: ident:" + f.getKey() + ", status:" + f.getValue());
        }
      }
      return initContext = PegContext.initWithParams(globals, locals);
    }


    /**
     * Create the PegNode and resulting Context for the expression.
     *
     * NOTE: This implementation assumes that every mutant has an identical context as the original,
     * and that the referenced identifiers are a subset of the referenced identifiers in the original
     * maximal expression.
     */
    void computePegNodes() {
        tree = StaticJavaParser.parseExpression(source);
        ExpressionResult expressionResult;
        try {
          expressionResult = tree.accept(pev, getInitCtx());
        } catch (RuntimeException e) {
          pegTranslationError = true;
          logPegTranslationError(e.getMessage(), source);
          return;
        }
        try {
          peg = PegNode.maxExpr(startPos, expressionResult.peg.id, expressionResult.context);
          context = expressionResult.context;
          for (Mutant m : mutants) {
            m.computePegNodes(initContext);
          }
        }
        catch (NullPointerException e) {
          throw e;
        }
    }

    @Override
    public String toString() {
      return "MaxExpr{" +
              "ns='" + ns + '\'' +
              ", orig='" + source + '\'' +
              ", mutants=" + mutants +
              '}';
    }
  }

  /**
   * Represent a mutant along with it's mutant id
   */
  class Mutant {
    MaxExpr maxExpr;
    String source;
    Expression tree;
    PegNode peg;
    PegContext context;
    int m_no;

    Mutant(MaxExpr maxExpr, String m_no, String source) {
      this(maxExpr, Integer.parseInt(m_no), source);
    }

    Mutant(MaxExpr maxExpr, int m_no, String source) {
      this.m_no = m_no;
      this.source = source;
      this.maxExpr = maxExpr;
      try {
        tree = StaticJavaParser.parseExpression(source);
        mutantsLogged++;
      } catch (RuntimeException e) {
        System.out.println("m_no: " + m_no);
        System.out.println("(" + (++errors) + ") Problem parsing mutant source " + source);
        System.out.println(trimParseErrorMessage(e.getMessage()));
        throw e;
      }
    }

    void computePegNodes(PegContext initCtx) {
      final ExpressionResult expressionResult = tree.accept(pev, initCtx);
      peg = PegNode.maxExpr(maxExpr.startPos, expressionResult.peg.id, expressionResult.context);
      context = expressionResult.context;
    }
  }

  private String trimParseErrorMessage(String msg) {
    int idx = msg.indexOf("\nWas expecting one of:");
    if (idx > 0) {
      return msg.substring(0, idx).trim();
    }
    return msg.trim();
  }
}
