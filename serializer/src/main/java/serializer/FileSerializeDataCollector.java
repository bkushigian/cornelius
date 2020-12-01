package serializer;

import java.util.*;

/**
 * Point this at a collection of files and try to serialize each method
 */
public class FileSerializeDataCollector {
  final List<String> worklist;
  final Set<ClassVisitResult> results = new HashSet<>();
  public static void main(String[] args) {
    final List<String> worklist = new ArrayList<>();

    for (final String arg : args ){
      if (arg.startsWith("@")) {
        throw new RuntimeException("Parsing arg files isn't implemented");
        // todo: parse file
      }
      else {
        worklist.add(arg);
      }
    }
  }

  public FileSerializeDataCollector(List<String> worklist) {
    this.worklist = worklist;
  }

  public Set<ClassVisitResult> run() {
    for (final String file : worklist) {

    }
    return null;
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
    final HashMap<String, Optional<Throwable>> methodFailureReasons = new HashMap<>();

    ClassVisitResult(final String fileName) {
      this.fileName = fileName;
    }
  }

}
