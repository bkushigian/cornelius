package serializer;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
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
    final File serializedDirectory = setupSerializedDirectory("serialized-directory");

    for (File origFile : files ){
      try {
        final CompilationUnit cu = StaticJavaParser.parse(origFile);
        final XMLGenerator xmlGen = new XMLGenerator();
        final SimpleJavaToPegTranslator translator = new SimpleJavaToPegTranslator();
        final Map<String, PegNode> methodMap = translator.translate(cu);

        boolean mutatedThisFile = false;
        // Iterate through each method
        for (String sig : mutantsLog.methodNameMap.keySet()){
          if (!sig.contains("@")) continue;  // TODO: handle class-level mutations
          final String canonical = Util.canonicalizeMajorName(sig);
          if (!methodMap.containsKey(canonical)) {
            continue; // This method was not mutated
          }
          mutatedThisFile = true;
          final String sourceFile = sig.substring(0, sig.indexOf('@')).replace('.', '/') + ".java";
          System.out.println("================================================================================");
          System.out.println("[+] Visiting method signature: " + sig);
          System.out.println("[+] Source file: " + sourceFile);
          System.out.println("[+] Name: " + canonical);

          // Get all rows from MutantsLog corresponding to this method
          final Set<MutantsLog.Row> rowsForMethod = mutantsLog.methodNameMap.get(sig);

          List<MutantsLog.Row> rowsToAdd = new ArrayList<>();
          for (MutantsLog.Row row : rowsForMethod) {
            final File mutantFile = idToFiles.get(row.id);
            try {
              final CompilationUnit mcu = StaticJavaParser.parse(mutantFile);
              try {
                row.pegId = translator.translate(mcu, canonical)
                        .orElseThrow(() -> new RuntimeException("Couldn't find mutant"))
                        .id;
                rowsToAdd.add(row);
              } catch (IllegalStateException e) {
                System.err.println("erroneous mutant id: " + row.id);
                throw e;
              } catch (RuntimeException e) {

              }
            } catch (FileNotFoundException e) {
              throw new RuntimeException("Couldn't find mutant " + row.id);
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

        final Path filepath = Paths.get(serializedDirectory.getName(), origFile.getName().replace("/", "_")
                .replace(".java", "--serialized.xml"));
        final String filename = filepath.toString();
        System.out.println("Creating serialized file: " + filename);
        xmlGen.writeToFile(filename);
      //} catch (FileNotFoundException e) {
      //  e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
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
