package serializer.peg.visitor;

import serializer.peg.PegNode;

import java.util.HashMap;

public class PegDFSEnumeratorVisitor extends PegVisitor<Void, HashMap<Integer, Integer>>  {

  private int i = 0;

  @Override
  public Void visit(PegNode.ThetaNode node, HashMap<Integer, Integer> arg) {
    arg.put(node.id, i++);
    return null;
  }
}
