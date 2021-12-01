package serializer;

import com.github.javaparser.Problem;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.CompilationUnit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class TryToParseMutants {

  String mutantsDirectory = "mutants";
  public static void main(String[] args) {

    TryToParseMutants ttpm = new TryToParseMutants();
    if (args.length == 1) ttpm.mutantsDirectory = args[0];
    ttpm.run();
  }

  public void run() {
    final Map<String, File> idToFiles = Util.collectMutantFiles(new File(mutantsDirectory));
    final Set<File> files = new HashSet<>(idToFiles.values());
    Util.ProgressBar bar = new Util.ProgressBar(files.size());
    Map<File, com.github.javaparser.ParseProblemException> failedMutantParseFiles = new HashMap<>();

    long i = 0;
    for (File origFile : files ){
      bar.printBar(i++);
      try {
        final CompilationUnit cu = StaticJavaParser.parse(origFile);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } catch (com.github.javaparser.ParseProblemException e) {
        failedMutantParseFiles.put(origFile, e);
      }
    }
    bar.printBar(i, "\n");
    i = 1;
    List<File> keys = new ArrayList<File>(failedMutantParseFiles.keySet());
    keys.sort((f1, f2) -> {
      try {
        return f1.getCanonicalPath().compareTo(f2.getCanonicalPath());
      } catch (IOException e) {
        return f1.getName().compareTo(f2.getName());
      }
    });
    for (File key : keys) {
      final com.github.javaparser.ParseProblemException e = failedMutantParseFiles.get(key);
      try {
        System.out.printf("%d: %s\n", i++, key.getCanonicalPath());
      } catch (IOException ex) {
        System.out.printf("%d: Failed to get cannonical path...here's what I can give you: %s\n", i++, key);
      }
      for (Problem p : e.getProblems()) {
        Optional<TokenRange> location = p.getLocation();
        location.ifPresent(javaTokens -> System.out.println("    " + javaTokens.getBegin().getRange()));
      }
    }
  }
}
