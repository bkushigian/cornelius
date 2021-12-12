package serializer;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.expr.Expression;
import serializer.peg.*;
import serializer.xml.XMLGenerator;

import javax.xml.transform.TransformerException;
import java.io.*;
import java.util.regex.Pattern;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ExprSerializer {
  int errors = 0;
  int mutantsLogged = 0;
  int maxExprsLogged = 0;
  int skippedMutants = 0;
  final String exprsDir;
  final PegExprVisitor pev = new PegExprVisitor();

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.err.println("usage: java ExprSerializer exprs-dir");
      System.exit(1);
    }

    ExprSerializer serializer = new ExprSerializer(args[0]);
    serializer.serialize();
  }

  ExprSerializer(String exprsDir) {
    this.exprsDir = exprsDir;
  }

  void serialize() throws IOException {
    final File exprsDir = new File(this.exprsDir);
    if (!exprsDir.isDirectory()) {
      throw new RuntimeException("Invalid expression directory: " + exprsDir);
    }
    File[] dirs = Util.setUpOutputDirectory("cornelius", "exprs", "exprs/subjects", "exprs/logs");
    final File subjectsDir = dirs[2];
    final File logsDir = dirs[3];

    final File[] files = exprsDir.listFiles(f -> f.isFile() && f.getName().endsWith(".expr"));
    for (File file : files) {
      PegNode.clear();
      ExprFile ef = new ExprFile(file);
      ef.writeToCorFile(subjectsDir);
    }
    System.out.println("Serialized " + maxExprsLogged + " maximal expressions");
    System.out.println("Serialized " + mutantsLogged + " mutant expressions");
    System.out.println("Skipped " + skippedMutants + " mutant expressions (couldn't parse the original expression)");
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
            throw new RuntimeException("Unknown status of " + f.getKey());
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
