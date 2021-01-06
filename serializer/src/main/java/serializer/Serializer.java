package serializer;

import com.github.javaparser.Problem;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import serializer.peg.*;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Serializer {

  public static void main(String[] args) {
    if (args.length < 2) {
      usage();
    }

    Serializer serializer = new Serializer(args);
    serializer.run();
  }

  private String mutantLogPath;
  private String mutantsDirectory;
  private List<File> files = new ArrayList<>();

  private Serializer(String[] args) {
    parseArgs(args);
  }

  private void parseArgs(String[] args) {
    mutantLogPath = args[0];
    mutantsDirectory = args[1];
    for (int i = 2; i < args.length; ++i) {
      final String arg = args[i];
      if (arg.startsWith("@")) {
        final String argFile = arg.substring(1);
        try {
          BufferedReader in = new BufferedReader(new FileReader(argFile));
          String st;
          while ((st = in.readLine()) != null) {
            if (!st.trim().isEmpty()) files.add(new File(st.trim()));
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      else {
        files.add(new File(arg));
      }
    }
    System.out.printf("Found %d files to visit\n", files.size());
  }

  public static void usage() {
    System.err.println("Usage: Serializer mutant-log mutants-dir java-file*");
    System.exit(1);
  }

  public void run() {
    final MutantsLog mutantsLog = new MutantsLog(mutantLogPath);
    final Map<String, File> idToFiles = Util.collectMutantFiles(new File(mutantsDirectory));
    final File serializedDirectory = setupSerializedDirectory("subjects");

    Util.ProgressBar bar = new Util.ProgressBar(files.size());
    System.out.println("Serializing files...");
    Map<File, com.github.javaparser.ParseProblemException> failedMutantParseFiles = new HashMap<>();

    long i = 0;
    for (File origFile : files ){
      bar.printBar(i++);
      try {
        final CompilationUnit cu = StaticJavaParser.parse(origFile);
        final XMLGenerator xmlGen = new XMLGenerator();
        final SimpleJavaToPegTranslator translator = new SimpleJavaToPegTranslator();
        final Map<String, PegNode> methodMap = translator.translate(cu);

        boolean mutatedThisFile = false;
        // Iterate through each method
        for (String sig : mutantsLog.methodNameMap.keySet()){
          System.out.println("SIG: " + sig);
          if (!sig.contains("@")) continue;  // TODO: handle class-level mutations
          final String canonical = Util.canonicalizeMajorName(sig);
          if (!methodMap.containsKey(canonical)) {
            System.out.println("continuing: " + canonical);
            for (String s : methodMap.keySet()) {
              System.out.println(s);
            }

            continue; // This method was not mutated
          }
          mutatedThisFile = true;
          final String sourceFile = sig.substring(0, sig.indexOf('@')).replace('.', '/') + ".java";

          // Get all rows from MutantsLog corresponding to this method
          final Set<MutantsLog.Row> rowsForMethod = mutantsLog.methodNameMap.get(sig);

          List<MutantsLog.Row> rowsToAdd = new ArrayList<>();
          for (MutantsLog.Row row : rowsForMethod) {
            System.out.println(row);
            final File mutantFile = idToFiles.get(row.id);
            try {
              final CompilationUnit mcu = StaticJavaParser.parse(mutantFile);
              try {
                row.pegId = translator.translate(mcu, canonical)
                        .orElseThrow(() -> new RuntimeException("Couldn't find mutant"))
                        .id;
                rowsToAdd.add(row);
              } catch (RuntimeException e) {}
            } catch (FileNotFoundException e) {
              throw new RuntimeException("Couldn't find mutant " + row.id);
            } catch (com.github.javaparser.ParseProblemException e) {
              failedMutantParseFiles.put(mutantFile, e);
            }
          }
          if (!rowsToAdd.isEmpty()) {
            xmlGen.addSubject(origFile.getName(), sig, methodMap.get(canonical).id);
            for (MutantsLog.Row row : rowsToAdd) {
              xmlGen.addMutant(sig, row.id, row.pegId);
            }
          }
        }
        if (!mutatedThisFile) continue;

        // TODO: this involves giving public access to the idLookup which is sketchy.
        xmlGen.addDeduplicationTable(PegNode.getIdLookup());

        Optional<PackageDeclaration> packageDeclaration = cu.getPackageDeclaration();
        String pkgString = "";
        if (packageDeclaration.isPresent()) {
          pkgString = packageDeclaration.get().getName().toString() + "::";
        }
        final String serializedFilename = pkgString + origFile.getName().replace(".java", ".cor");
        final Path filepath = Paths.get(serializedDirectory.getName(), serializedFilename);
        final String filename = filepath.toString();
        bar.clearLastBar();
        System.out.println("Creating serialized file: " + filename);
        xmlGen.writeToFile(filename);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    bar.clearLastBar();
    bar.printBar(i);

    i = 1;
    for (Map.Entry<File, com.github.javaparser.ParseProblemException> entry : failedMutantParseFiles.entrySet()) {
      final File f = entry.getKey();
      final com.github.javaparser.ParseProblemException e = entry.getValue();
      try {
        System.out.printf("%d: %s\n", i++, f.getCanonicalPath());
      } catch (IOException ex) {
        System.out.printf("%d: Failed to get cannonical path...here's what I can give you: %s\n", i++, f);
      }
      for (Problem p : e.getProblems()) {
        Optional<TokenRange> location = p.getLocation();
        location.ifPresent(javaTokens -> System.out.println("    " + javaTokens.getBegin().getRange()));
      }
    }
  }

  private File setupSerializedDirectory(final String path) {
    final File serializedDirectory = new File(path);
    try {
      Util.recursivelyDelete(serializedDirectory);
      serializedDirectory.mkdirs();
    } catch (IOException e) {
      System.err.println("Failed to delete serialized directory " + serializedDirectory);
      System.err.println("Exiting with status 1");
      System.exit(1);
    }
    return serializedDirectory;
  }
}
