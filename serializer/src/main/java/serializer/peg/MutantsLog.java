package serializer.peg;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * This class represents the {@code mutants.log} file. In particular, this comprises a list of {@code Row}s as
 * well as two convenience data structures:
 *
 * <ol>
 *     <li>A map from mutant id to its corresponding {@code Row}</li>
 *     <li>A map from method name to a {@code Set<Row>}} containing all {@code Row}s corresponding to a mutation
 *         of the method</li>
 * </ol>
 *
 */
public class MutantsLog {

    /**
     * {@code idMap} maps mutant Ids (in a {@code String} form, i.e. "1", "2", ...) to the corresonding {@code Row}
     * object that was parsed from {@code mutants.log}
     */
    public final Map<String, Row> idMap;

    /**
     * {@code methodNameMap} maps method names to the set of mutants that mutate the method.
     */
    public final Map<String, Set<Row>> methodNameMap;

    /**
     * A list of all rows from {@code mutants.log}
     */
    public final List<Row> rows;

    final String logPath;

    public MutantsLog(final String logPath) {
        this.logPath = logPath;
        idMap = new HashMap<>();
        methodNameMap = new HashMap<>();
        rows = new ArrayList<>(128);

        // Read in mutants.log, creating Row objects
        System.out.println("Parsing mutant logs");
        final long starttime = System.currentTimeMillis();
        try (BufferedReader br = new BufferedReader(new FileReader(logPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                final Row r = new Row(line);
            }
            final long endtime = System.currentTimeMillis();
            System.out.printf("Parsed %d mutant log rows in %f seconds\n", rows.size(),
                    (endtime - starttime) / 1000.0d);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Represent a row from {@code mutants.log}
     */
    public class Row {

        /**
         * Mutant id generated by Major (i.e., {@code "1"}, {@code "2"}, ...
         */
        public final String id;

        /**
         * Mutation type (AOR, etc)
         */
        public final String mutationType;

        /**
         * The original unmutated node
         */
        public final String originalNode;

        /**
         * The mutated node
         */
        public final String mutantNode;

        /**
         * Fully qualified method signature: "Class@name(arg,types)"
         */
        public final String method;

        /**
         * Source code line number the mutant occurred at
         */
        public final int lineNumber;

        /**
         * String describing the transformation: {@code a + b |===> a - b}
         */
        public final String transformation;

        /**
         * A string representation of this mutants AST. This is computed by a visitor
         */
        public String ast;

        /**
         * Representation of the peg id that we generated while constructing the peg node
         */
        public int pegId;

        /**
         * Create a new {@code Row} from a line of raw text and enter the resulting values into {@code MutantsLog}
         * data structures.
         *
         * @param raw raw String from the log representing the row
         */
        Row(String raw) {
            final String[] items = raw.split(":");
            assert items.length == 7;
            id = items[0];
            mutationType = items[1];
            originalNode = items[2];
            mutantNode = items[3];
            method = items[4];
            lineNumber = Integer.parseInt(items[5]);
            transformation = items[6];

            rows.add(this);
            idMap.put(id, this);
            methodNameMap.putIfAbsent(method, new HashSet<>());
            methodNameMap.get(method).add(this);
        }

        @Override
        public String toString() {
            return "Row{" +
                    "id='" + id + '\'' +
                    ", mutationType='" + mutationType + '\'' +
                    ", originalNode='" + originalNode + '\'' +
                    ", mutantNode='" + mutantNode + '\'' +
                    ", method='" + method + '\'' +
                    ", lineNumber=" + lineNumber +
                    ", transformation='" + transformation + '\'' +
                    '}';
        }
    }
}
