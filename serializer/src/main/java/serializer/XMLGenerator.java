package serializer;//Based on example code from: https://examples.javacodegeeks.com/core-java/xml/parsers/documentbuilderfactory/create-xml-file-in-java-using-dom-parser-example/

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import serializer.peg.MutantsLog;
import serializer.peg.PegNode;

public class XMLGenerator {

    public static final String dirPath = System.getProperty("user.dir");

    // Map method signatures to their associated <subject> elements
    private Map<String, Element> methodToSubject =  new HashMap<>();

    private Element subjects;
    private Document document;
    private DocumentBuilder documentBuilder;
    private Transformer transformer;

    public XMLGenerator() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            documentBuilder = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException pce) {
            throw new RuntimeException("Couldn't create document builder");
        }
        document = documentBuilder.newDocument();

        //setup transofrmer
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            transformer = transformerFactory.newTransformer();
        } catch (TransformerConfigurationException tce) {
            tce.printStackTrace();
            throw new RuntimeException("Couldn't create transformer");
        }
        // Pretty print with indents
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        // Number of spaces
        // transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        // No longer needed since subjects is cut out of spec
        subjects = document.createElement("subjects");
        document.appendChild(subjects);
    }

    /**
     * This method creates a new {@code <subject>} element and stores it in a map associated with key {@code method}
     * @param sourceFile name of source file containing the original method
     * @param methodString fully qualified method signature Class@methodName(T1,T2,T3)
     * @param pegId: the peg id, produced by serialization, for the original method
     */
    public void addSubject(String sourceFile, String methodString, int pegId) {
        final Element subject = document.createElement("subject");
        subject.setAttribute("sourcefile", sourceFile);
        subject.setAttribute("method", methodString);
        subjects.appendChild(subject);

        final Element egg = document.createElement("egg");
        egg.appendChild(document.createTextNode(Integer.valueOf(pegId).toString()));
        subject.appendChild(egg);

        methodToSubject.put(methodString, subject);
    }

    /**
     * This method creates a new {@code <subject>} element and stores it in a map associated with key {@code method}
     * and adds all the associated mutants in {@code log} to the created subject.
     * @param sourceFile name of source file containing the original method
     * @param methodString fully qualified method signature Class@methodName(T1,T2,T3)
     * @param pegId: the peg id, produced by serialization, for the original method
     * @param log a list of log entries of mutants that have been serialized that we should add to this subject field
     */
    public void addSubject(String sourceFile, String methodString, int pegId, List<MutantsLog.Row> log) {
        addSubject(sourceFile, methodString, pegId);
        for (MutantsLog.Row row : log) {
            addMutant(methodString, row.id, row.pegId);
        }
    }

    /**
     *
     * @param methodString
     * @param mutantId
     * @param pegId
     */
    public void addMutant(String methodString, String mutantId, int pegId) {
        //todo: lookup to see if method has a subject associated with `method` in methodToSubject. If not, throw
        // an IllegalArgumentException.
        // If a subject has already been stored under key `method`, add a new <mutant> element to its <mutants>
        // with the provided data

        if (methodToSubject.containsKey(methodString)) {

            Element subject = methodToSubject.get(methodString); // does this actually get and modifiy the element in the map?

            Element mutant = document.createElement("mutant");
            mutant.setAttribute("id", mutantId);
            subject.appendChild(mutant);

            Element egg = document.createElement("egg");
            egg.appendChild(document.createTextNode(Integer.valueOf(pegId).toString()));
            mutant.appendChild(egg);
        }
        else {
            throw new IllegalArgumentException("Map does not contain associated subject for mutant");
        }
    }

    public boolean hasSubject() {
        return subjects.getElementsByTagName("subject").getLength() != 0;
    }

    public int numSubjects() {
        return subjects.getElementsByTagName("subject").getLength();
    }

    public void addDeduplicationTable(Map<Integer, PegNode> dedupTable) {
        Element table = document.createElement("id_table");
        subjects.appendChild(table);
        final List<Integer> keys = new ArrayList<>(dedupTable.keySet());
        keys.sort(null);
        for (Integer id : keys) {
            final PegNode p = dedupTable.get(id);
            Element dedupEntry = document.createElement("dedup_entry");
            table.appendChild(dedupEntry);
            dedupEntry.setAttribute("id", id.toString());
            dedupEntry.setAttribute("peg", p.toString());
        }
    }

    /**
     * Convert the built up document as a String
     * @return
     */
    public void writeToConsole() {
        // create the xml file
        DOMSource domSource = new DOMSource(document);
        StreamResult result = new StreamResult(System.out);
        try {
            transformer.transform(domSource, result);
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Write the XML file we've built to disk
     * @param filename

     * @throws IOException
     */
    public void writeToFile(String filename) throws IOException {
        try {
            DOMSource domSource = new DOMSource(document);
            StreamResult streamResult = new StreamResult(new File(dirPath + "/" + filename));
            transformer.transform(domSource, streamResult);
        } catch (TransformerException e){ // is this right?
            throw new RuntimeException("Failed to make new File " + dirPath + "/" + filename );  // check if this is
            // supposed to be
            // runtime or
            // ioexception
        }
    }
}