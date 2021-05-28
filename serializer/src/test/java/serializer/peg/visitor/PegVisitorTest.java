package serializer.peg.visitor;

import serializer.peg.PegNode;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class PegVisitorTest {
  static class TestPegVisitor extends PegVisitor<Void, Map<PegNode, Integer>> {
    @Override
    public Void visit(PegNode.ThetaNode node, Map<PegNode, Integer> arg) {
      throw new NotImplementedException();
    }

    @Override
    public Void visit(PegNode.StringLit node, Map<PegNode, Integer> arg) {
      throw new NotImplementedException();
    }

    @Override
    public Void visit(PegNode.PhiNode node, Map<PegNode, Integer> arg) {
      throw new NotImplementedException();
    }

    @Override
    public Void visit(PegNode.BoolLit node, Map<PegNode, Integer> arg) {
      throw new NotImplementedException();
    }

    @Override
    public Void visit(PegNode.OpNode node, Map<PegNode, Integer> arg) {
      throw new NotImplementedException();
    }

    @Override
    public Void visit(PegNode.IntLit node, Map<PegNode, Integer> arg) {
      throw new NotImplementedException();
    }
  }

}