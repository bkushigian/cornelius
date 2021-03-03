package serializer.peg;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Serialize Java files to PEGs. This doesn't handle mutants (see {@code PegSubjectSerializer})
 */
public class PegSerializer {

  /**
   * Run from CLI, this takes java files to be serialized as args and outputs their serialized form
   * @param args array of java source files to be serialized
   */
  public static void main(String[] args) {
    final PegSerializer s = new PegSerializer(".");
    boolean scrapeComments = false;
    List<String> files = new ArrayList<>();
    for (String arg : args) {
      if ("--scrape-comments".equals(arg)) {
        scrapeComments = true;
      }
      else {
        files.add(arg);
      }
    }

    for (String file : files) {
      try {
        final Map<String, PegNode> translated = s.serialize(file, scrapeComments);
        System.out.println("* " + file);
        for (final String meth : translated.keySet()) {
          final PegNode node = translated.get(meth);
          System.out.printf("%s: %s\n", meth, node.toDerefString());
        }
      } catch (FileNotFoundException e) {
        System.out.println("Couldn't load file " + file);
      }
    }
  }

  /**
   * Serialize a Java file
   * @param javaFile the Java source file to translate
   * @return a mapping from method names to resulting pegs
   * @throws FileNotFoundException if
   */
  public Map<String, PegNode> serialize(final String javaFile) throws FileNotFoundException {
    return serialize(javaFile, false);
  }

  /**
   * Serialize a Java file
   * @param javaFile the Java source file to translate
   * @param scrapeComments should comments be scraped for test pair generation?
   * @return a mapping from method names to resulting pegs
   * @throws FileNotFoundException if
   */
  public Map<String, PegNode> serialize(final String javaFile, boolean scrapeComments) throws FileNotFoundException {
    final CompilationUnit cu = StaticJavaParser.parse(new File(javaFile));
    final PegTranslator t = new PegTranslator(scrapeComments);
    return t.translate(cu);
  }

  /**
   * Directory to output serialized PEGs
   */
  final String outputDirectory;
  public PegSerializer(final String outputDir) {
    this.outputDirectory = outputDir;
  }
}
