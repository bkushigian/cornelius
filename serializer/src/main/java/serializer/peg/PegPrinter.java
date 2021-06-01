package serializer.peg;

import serializer.peg.visitor.PegVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PegPrinter {

  public static String pegToString(PegNode peg) {
    ToStringVisitor tsv = new ToStringVisitor();
    final String pegString = peg.accept(tsv, null);
    final String table = tsv.identificationTable();
    return String.format("Peg: %s\nIdentifications: %s", pegString, table);
  }

  /**
   * Visitor used by PegPrinter to create a string
   */
  static class ToStringVisitor extends PegVisitor<String, Void> {

    private int blankId = 0;
    /**
     * Map tracking id assigned to each blank.
     */
    final private Map<PegNode.BlankNode, Integer> blankToId = new HashMap<>();

    /**
     * An in-order list of visited blank nodes. The index of a blank node is equal to its blankId
     * stored in {@code blankToId}
     */
    final private List<PegNode.BlankNode> visitedBlanks = new ArrayList<>();

    @Override
    protected String combine(PegNode.IntLit node, Void arg) {
      return String.format("(int-lit %d)", node.value);
    }

    @Override
    protected String combine(PegNode.BoolLit node, Void arg) {
      return String.format("(bool-lit %b)", node.value);
    }

    @Override
    protected void preVisit(PegNode.BlankNode node, Void arg) {
      blankToId.put(node, blankId++);
      visitedBlanks.add(node);
      table.put(node, String.format("blank[%d]", blankId));
    }

    @Override
    protected String combine(PegNode.BlankNode node, Void arg) {
      // Precondition: `this.preVisit(node)` has been called, populating `this.table`
      return table.get(node);
    }

    @Override
    protected String combine(PegNode.BlankNode node, Void arg, PegNode identified) {
      // Precondition: `this.preVisit(node)` has been called, populating `this.table`

      return table.get(node);
    }

    @Override
    protected String combine(PegNode.StringLit node, Void arg) {
      return String.format("(string-lit %s)", node.value);
    }

    @Override
    protected String combine(PegNode.OpNode node, Void arg, List<String> children) {
      StringBuilder sb = new StringBuilder("(");
      sb.append(node.op);
      for (String child : children) {
        sb.append(" ");
        sb.append(child);
      }
      return sb.append(")").toString();
    }

    @Override
    protected String combine(PegNode.ThetaNode node, Void arg, String init, String continuation) {
      return String.format("(theta %s %s)", init, continuation);
    }

    @Override
    protected String combine(PegNode.PhiNode node, Void arg, String guard, String thn, String els) {
      return String.format("(phi %s %s %s)", guard, thn, els);
    }

    public String identificationTable() {
      StringBuilder sb = new StringBuilder("(");
      for (int blankId = 0; blankId < visitedBlanks.size(); ++blankId) {
        final PegNode.BlankNode blank = visitedBlanks.get(blankId);
        blank.getIdentifiedNode().ifPresent(n -> sb.append(
                String.format("\n  (<=> %s %s)", table.get(blank), table.get(n))));
      }
      return sb.append(')').toString();
    }
  }
}
