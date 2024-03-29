package serializer.peg;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;

import serializer.Util;
import serializer.xml.XMLGenerator;

import javax.xml.transform.TransformerException;

/**
 * This class provides a CLI to serialize a program and its mutants into an XML file.
 */
public class PegSubjectSerializer {
    boolean printDerefStrings = false;

    public static void main(String[] args) {
        if (args.length < 2) {
            usage("Incorrect arg count");
        }
        final PegSubjectSerializer m =  new PegSubjectSerializer(
                String.format("%s/%s", args[0], args[1]),
                String.format("%s/mutants", args[0]),
                String.format("%s/mutants.log", args[0]));

        m.parseArgs(args);
        m.run();
    }

    private void parseArgs(String[] args) {
        for (String arg : args) {
            if ("--print-pegs".equals(arg)) {
                printDerefStrings = true;
                break;
            }
        }
    }

    /**
     * The path of the original programs Java source code
     */
    final String origPath;

    /**
     * The path to the mutants directory generated by Major. This directory should only contain subdirectories
     * 1, 2, 3, ..., n, where each subdirectory {@code i} is titled after the mutant-id of the mutant it contains.
     * That is, directory 17 contains the mutant with mutant id 17 as documented in the {@code mutants.log} file.
     */
    final String mutantsPath;

    /**
     * The path to {@code mutants.log}
     */
    final String logPath;

    public PegSubjectSerializer(final String orig, final String mutants, final String logPath) {
        origPath = orig;
        mutantsPath = mutants;
        this.logPath = logPath;
    }

    /**
     * Serialize a SIMPLE Java program's Peg representation
     */
    public void run() {
        try {
            final CompilationUnit cu = StaticJavaParser.parse(new File(origPath));
            final MutantsLog mutantsLog = new MutantsLog(logPath);
            final XMLGenerator xmlGen = new XMLGenerator();
            final PegTranslator translator = new PegTranslator();

            final Map<String, PegNode> methodMap = translator.translate(cu);

            // Iterate through each method
            for (String sig : mutantsLog.methodNameMap.keySet()){
                // TODO: handle class-level mutations
                if (!sig.contains("@")) continue;
                final String canonical = Util.canonicalizeMajorName(sig);
                final String sourceFile = sig.substring(0, sig.indexOf('@')).replace('.', '/') + ".java";
                if (!methodMap.containsKey(canonical)) {
                    continue;
                }
                xmlGen.addSubject(origPath, sig, methodMap.get(canonical).id);

                System.out.println("================================================================================");
                System.out.println("[+] Visiting method signature: " + sig);
                System.out.println("[+] Source file: " + sourceFile);
                System.out.println("[+] Name: " + canonical);

                final Set<MutantsLog.Row> rows = mutantsLog.methodNameMap.get(sig);
                if (printDerefStrings) {
                    System.out.printf("original: %s\n", methodMap.get(canonical).toDerefString());
                }

                for (MutantsLog.Row row : rows) {
                    final Set<File> javaFiles = new HashSet<>();
                    Util.collectJavaFiles(new File(Paths.get(mutantsPath, row.id).toString()), javaFiles);
                    if (javaFiles.size() != 1) {
                        System.err.println(Arrays.toString(javaFiles.toArray()));
                        throw new IllegalStateException(
                                String.format("Mutant root %s contained %d java files, expected1",
                                        Paths.get(mutantsPath, row.id).toString(),
                                        javaFiles.size()));
                    }

                    try {
                        final CompilationUnit mcu = StaticJavaParser.parse(javaFiles.iterator().next());
                        try {
                            row.pegId = translator.translate(mcu, canonical).id;
                        } catch (IllegalStateException e) {
                          System.err.println("erroneous mutant id: " + row.id);
                          throw e;
                        }

                        xmlGen.addMutant(sig, row.id, row.pegId);
                        if (printDerefStrings) {
                            System.out.printf("mutant %s: %s\n", row.id,
                                    PegNode.idLookup(row.pegId).map(PegNode::toDerefString));
                        }

                    } catch (FileNotFoundException e) {
                        throw new RuntimeException("Couldn't parse mutant " + row.id);
                    }
                }
            }

            // TODO: this involves giving public access to the idLookup which is sketchy.
            xmlGen.addIdTable(PegNode.getIdLookup());
            xmlGen.addEquivalences(PegNode.getNodeEquivalences());

            xmlGen.writeToFile("subjects.xml");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Print an error message and usage info to stderr and exit with status 1.
     * @param msg error message to print to stderr
     */
    static void usage(final String msg) {
        System.err.println(msg);
        System.err.println("usage: PegSubjectSerializer dir java_file [ARGS...]");
        System.err.println("    - dir: Base directory of subject. This should contain the java file, mutants, and " +
                "mutants.log");
        System.err.println("    - java_file: name of the java file to be compiled");
        System.err.println("ARGS");
        System.err.println("----");
        System.err.println("    --print-pegs: Print each PEG");

        System.exit(1);
    }
}
