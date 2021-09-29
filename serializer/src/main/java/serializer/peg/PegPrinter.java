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

    private int thetaId = 0;
    /**
     * Map tracking id assigned to each theta.
     */
    final private Map<PegNode.ThetaNode, Integer> thetaToId = new HashMap<>();

    /**
     * An in-order list of visited theta nodes. The index of a theta node is equal to its thetaId
     * stored in {@code thetaToId}
     */
    final private List<PegNode.ThetaNode> visitedThetas = new ArrayList<>();

    @Override
    protected String combine(PegNode.IntLit node, Void arg) {
      return String.format("(int-lit %d)", node.value);
    }

    @Override
    protected String combine(PegNode.BoolLit node, Void arg) {
      return String.format("(bool-lit %b)", node.value);
    }

    @Override
    protected void preVisit(PegNode.ThetaNode node, Void arg) {
      thetaToId.put(node, thetaId++);
      visitedThetas.add(node);
      table.put(node, String.format("theta[%d]", thetaId));
    }

    @Override
    protected String combine(PegNode.ThetaNode node, Void arg, String init) {
      // Precondition: `this.preVisit(node)` has been called, populating `this.table`
      
      return String.format("(%s %s)", table.get(node), init);
    }

    @Override
    protected String combine(PegNode.ThetaNode node, Void arg, String init, PegNode identified) {
      // Precondition: `this.preVisit(node)` has been called, populating `this.table`

      return String.format("(%s %s)", table.get(node), init);
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
    protected String combine(PegNode.PhiNode node, Void arg, String guard, String thn, String els) {
      return String.format("(phi %s %s %s)", guard, thn, els);
    }

    public String identificationTable() {
      StringBuilder sb = new StringBuilder("(");
      for (int thetaId = 0; thetaId < visitedThetas.size(); ++thetaId) {
        final PegNode.ThetaNode theta = visitedThetas.get(thetaId);
        theta.getContinuation().ifPresent(n -> sb.append(
                String.format("\n  (<=> %s %s)", table.get(theta), table.get(n))));
      }
      return sb.append(')').toString();
    }
  }
}
