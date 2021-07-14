package serializer.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.*;
import org.xml.sax.SAXException;
import serializer.peg.PegNode;

/**
 * This class reads an XML file describing a subject and produces pegs.
 *
 */
public class XMLReader {
  /**
   * Read a .cor file's contents and parse into PegNodes.
   * @param corFileContents
   * @return a list of subjects mapping mutant ids to peg ids
   */
  public List<Subject> readCorFileContents(String corFileContents) {
    try {
      ByteArrayInputStream inputStream = new ByteArrayInputStream(corFileContents.getBytes("UTF-8"));
      final Document doc = DocumentBuilderFactory
              .newInstance()
              .newDocumentBuilder()
              .parse(inputStream);
      return docToSubjects(doc);
    } catch (ParserConfigurationException e) {
      throw new RuntimeException("Parser configuration exception: " + e.getMessage());
    } catch (SAXException e) {
      throw new RuntimeException("A parse error occurred: " + e.getMessage());
    } catch (IOException e) {
      throw new RuntimeException("An IO exception occurred: " + e.getMessage());
    }
  }

  final Map<String, Integer> stringIdToPegnodeId = new HashMap<>();
  final Map<String, String> idToName = new HashMap<>();

  Integer lookup(String id) {
    if (! stringIdToPegnodeId.containsKey(id)) throw new IllegalArgumentException("Id " + id + " not in lookup table");
    return stringIdToPegnodeId.get(id);
  }

  /**
   * Translate a doc, parsed from a valid .cor file, into PegNodes and return a List of Subjects pointing
   * to the parsed Peg Ids and tracking metadata.
   * @param doc
   * @return
   */
  private List<Subject> docToSubjects(Document doc) {
    final List<Subject> subjects = new ArrayList<>();
    final NodeList idTable = doc.getElementsByTagName("dedup_entry");

    // First, read in the deduplicated entries
    for (int i = 0; i < idTable.getLength(); ++i) {
      final NamedNodeMap attributes = idTable.item(i).getAttributes();
      final String id = attributes.getNamedItem("id").getTextContent();
      final String peg = attributes.getNamedItem("peg").getTextContent();
      final char c = peg.charAt(0);
      if (c == '(') {
        stringIdToPegnodeId.put(id, parseOpNode(peg).id);
      }
      else if (('0' <= c && c <= '9') || c == '-' || c == '+') {
        stringIdToPegnodeId.put(id, PegNode.intLit(Integer.parseInt(peg)).id);
      } else if (('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z')) {
        // todo: parse name
        idToName.put(id, peg);
      } else {
        throw new IllegalStateException("Illegal first character: " + c);
      }
    }

    final NodeList nlSubjects = doc.getElementsByTagName("subject");

    // Next, read the subjects
    for (int i = 0; i < nlSubjects.getLength(); ++i) {
      if (! (nlSubjects.item(i) instanceof Element)) {
        throw new IllegalStateException();
      }
      final Element subjectElement = (Element)nlSubjects.item(i);
      final NamedNodeMap attributes = subjectElement.getAttributes();
      final String method = attributes.getNamedItem("method").getTextContent();
      final String sourcefile = attributes.getNamedItem("sourcefile").getTextContent();

      final Subject subject = new Subject(method, sourcefile);
      subjects.add(subject);

      final NodeList children = subjectElement.getChildNodes();

      for (int j = 0; j < children.getLength(); ++j) {
        final Node child = children.item(j);
        switch (child.getNodeName()) {
          case "pid": {
            Element pidElement = (Element)child;
            subject.setOriginalPid(lookup(pidElement.getTextContent()));
            break;
          }

          case "mutant": {
            Element mutantElement = (Element)child;
            final String mid = mutantElement.getAttributes().getNamedItem("mid").getTextContent();
            final Integer pid = lookup(mutantElement.getAttributes().getNamedItem("pid").getTextContent());
            subject.setMutantPid(mid, pid);
            break;
          }
        }
      }
    }

    return subjects;
  }

  private PegNode parseOpNode(String peg) {
    assert peg.startsWith("(") && peg.endsWith(")");
    String[] args = peg.substring(1, peg.length() - 1).split(" ");
    assert args.length != 0;
    final String op = args[0];
    switch (op) {
      case "phi":
        if (args.length != 4) {
          throw new IllegalArgumentException("Invalid phi node: expected 4 arguments but found: " + Arrays.toString(args));
        }
        return PegNode.phi(lookup(args[1]), lookup(args[2]), lookup(args[3]));

      case "heap":
        if (args.length != 3) {
          throw new IllegalArgumentException("Invalid heap node: expected 3 arguments but found: " + Arrays.toString(args));
        }
        return PegNode.heap(lookup(args[1]), lookup(args[2]));

      case "theta":
        if (args.length != 2) {
          throw new IllegalArgumentException("Invalid theta node: expected 2 arguments but found: " + Arrays.toString(args));
        }
        // Node identifications aren't done here
        return PegNode.theta(lookup(args[1]));
      case "var":
        if (args.length != 2) {
          throw new IllegalArgumentException("Invalid var: expected 2 args but found: " + Arrays.toString(args));
        }
        return PegNode.var(args[1]);
      default: {
        Integer[] argIds = Arrays.stream(args)
                .skip(1)
                .map(this::lookup)
                .toArray(Integer[]::new);
        return PegNode.opNode(op, argIds);
      }
    }
  }

  /**
   * Represent a parsed subject
   */
  public static class Subject {
    public final String method;
    public final String sourcefile;
    private Integer originalPid = -1;
    private final Map<String, Integer> midToPid = new HashMap<>();

    public Subject(String method, String sourcefile) {
      this.method = method;
      this.sourcefile = sourcefile;
    }

    /**
     * Set the mutants Peg Id
     * @param mid mutant id string
     * @param pid peg id
     * @return the old peg id (if any) associated with mid, or {@code null} if none exists; should be {@code null}
     * during normal operating procedures
     */
    public Integer setMutantPid(String mid, Integer pid) {
      return midToPid.put(mid, pid);
    }

    /**
     * Set the original Peg Id
     * @param pid the peg id
     * @return the old peg id
     */
    public Integer setOriginalPid(Integer pid) {
      Integer old = originalPid;
      originalPid = pid;
      return old;
    }

    public Integer getMutantPid(String mid) {
      return midToPid.get(mid);
    }

    public Integer getOriginalPid() {
      return originalPid;
    }

    public Set<String> getMutantIds() {
      return new HashSet<>(midToPid.keySet());
    }
  }
}
