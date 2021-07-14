package serializer;

import org.junit.Test;
import serializer.peg.PegNode;
import serializer.xml.XMLReader;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class XMLGeneratorTest {

  @Test
  public void test01() {
    XMLReader reader = new XMLReader();
    List<XMLReader.Subject> subjects = reader.readCorFileContents(testInput);
    System.out.println(subjects.size());
    assertEquals(1, subjects.size());
    XMLReader.Subject s = subjects.get(0);
    assertEquals(6, s.getMutantIds().size());
    Optional<PegNode> orig = PegNode.idLookup(s.getOriginalPid());
    assertTrue(orig.isPresent());
    assertTrue(orig.get().asOpNode().isPresent());
    PegNode.OpNode returnNode = orig.get().asOpNode().get();
    assertEquals("return-node", returnNode.op);

  }

  final String testInput = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
          "<subjects>\n" +
          "<subject method=\"TestCode@testMethod(int)\" sourcefile=\"TestCode.java\">\n" +
          "<pid>9</pid>\n" +
          "<mutant mid=\"1\" pid=\"11\"/>\n" +
          "<mutant mid=\"2\" pid=\"14\"/>\n" +
          "<mutant mid=\"3\" pid=\"21\"/>\n" +
          "<mutant mid=\"4\" pid=\"23\"/>\n" +
          "<mutant mid=\"5\" pid=\"25\"/>\n" +
          "<mutant mid=\"6\" pid=\"28\"/>\n" +
          "</subject>\n" +
          "<id_table>\n" +
          "<dedup_entry id=\"0\" peg=\"(this)\"/>\n" +
          "<dedup_entry id=\"1\" peg=\"(var 0)\"/>\n" +
          "<dedup_entry id=\"2\" peg=\"(a)\"/>\n" +
          "<dedup_entry id=\"3\" peg=\"(var 2)\"/>\n" +
          "<dedup_entry id=\"4\" peg=\"0\"/>\n" +
          "<dedup_entry id=\"5\" peg=\"(unit)\"/>\n" +
          "<dedup_entry id=\"6\" peg=\"(heap 4 5)\"/>\n" +
          "<dedup_entry id=\"7\" peg=\"1\"/>\n" +
          "<dedup_entry id=\"8\" peg=\"(+ 3 7)\"/>\n" +
          "<dedup_entry id=\"9\" peg=\"(return-node 8 6)\"/>\n" +
          "<dedup_entry id=\"10\" peg=\"(+ 3 4)\"/>\n" +
          "<dedup_entry id=\"11\" peg=\"(return-node 10 6)\"/>\n" +
          "<dedup_entry id=\"12\" peg=\"-1\"/>\n" +
          "<dedup_entry id=\"13\" peg=\"(+ 3 12)\"/>\n" +
          "<dedup_entry id=\"14\" peg=\"(return-node 13 6)\"/>\n" +
          "<dedup_entry id=\"15\" peg=\"(== 7 4)\"/>\n" +
          "<dedup_entry id=\"16\" peg=\"(java.lang.DivideByZeroError)\"/>\n" +
          "<dedup_entry id=\"17\" peg=\"(phi 15 16 5)\"/>\n" +
          "<dedup_entry id=\"18\" peg=\"(heap 4 17)\"/>\n" +
          "<dedup_entry id=\"19\" peg=\"(% 3 7)\"/>\n" +
          "<dedup_entry id=\"20\" peg=\"(phi 15 5 19)\"/>\n" +
          "<dedup_entry id=\"21\" peg=\"(return-node 20 6)\"/>\n" +
          "<dedup_entry id=\"22\" peg=\"(* 3 7)\"/>\n" +
          "<dedup_entry id=\"23\" peg=\"(return-node 22 6)\"/>\n" +
          "<dedup_entry id=\"24\" peg=\"(- 3 7)\"/>\n" +
          "<dedup_entry id=\"25\" peg=\"(return-node 24 6)\"/>\n" +
          "<dedup_entry id=\"26\" peg=\"(/ 3 7)\"/>\n" +
          "<dedup_entry id=\"27\" peg=\"(phi 15 5 26)\"/>\n" +
          "<dedup_entry id=\"28\" peg=\"(return-node 27 6)\"/>\n" +
          "</id_table>\n" +
          "</subjects>\n";
}