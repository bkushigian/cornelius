package serializer.peg;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import java.io.File;
import java.io.FileNotFoundException;
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
    for (String javaFile : args) {
      try {
        final Map<String, PegNode> translated = s.serialize(javaFile);
        System.out.println("* " + javaFile);
        for (final String meth: translated.keySet()) {
          final PegNode node = translated.get(meth);
          System.out.printf("%s: %s\n", meth, node.toDerefString());



        }
      } catch (FileNotFoundException e) {
        System.out.println("Couldn't load file " + javaFile);
      }
    }
  }

  /**
   * Serialize a Java file
   * @param javaFile the Java source file to translate
   * @return a mapping from method names to
   * @throws FileNotFoundException if
   */
  public Map<String, PegNode> serialize(final String javaFile) throws FileNotFoundException {
    final CompilationUnit cu = StaticJavaParser.parse(new File(javaFile));
    final SimpleJavaToPegTranslator t = new SimpleJavaToPegTranslator();
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
