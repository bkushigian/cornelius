package serializer.peg.visitor;

import org.junit.Test;
import serializer.peg.PegNode;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class PegVisitorTest {

  final PreOrderEnumeratorVisitor v = new PreOrderEnumeratorVisitor();
  final PegNode zero = PegNode.intLit(0);
  final PegNode one = PegNode.intLit(1);
  final PegNode two = PegNode.intLit(2);
  final PegNode tru = PegNode.boolLit(true);
  final PegNode fls = PegNode.boolLit(false);

  // (theta[0] (int-lit 0) blank[0])
  final PegNode.ThetaNode theta0 = PegNode.theta(zero.id);

  // (phi (bool-lit true) (int-lit 1) (int-lit 2))
  final PegNode.PhiNode phi0 = PegNode.phi(tru.id, one.id, two.id);

  // (phi (bool-lit false)
  //      (phi (bool-lit true) (int-lit 1) (int-lit 2))
  //      (phi (bool-lit true) (int-lit 1) (int-lit 2)))
  final PegNode.PhiNode phi1 = PegNode.phi(fls.id, phi0.id, phi0.id);

  @Test
  public void testTheta() {
    final Map<PegNode, Integer> map = new HashMap<>();
    theta0.accept(v, map);
    assertOrder(map, theta0, zero);
  }

  @Test
  public void testPhiNodeWithRepeats() {
    final Map<PegNode, Integer> map = new HashMap<>();
    phi1.accept(v, map);
    assertOrder(map, phi1, fls, phi0, tru, one, two);
  }

  @Test
  public void testSelfReference() {
    // (theta 0 theta))
    final PegNode.ThetaNode theta = PegNode.theta(zero.id);
    theta.setContinuation(theta.id);

    final Map<PegNode, Integer> map = new HashMap<>();
    theta.accept(v, map);
    assertOrder(map, theta, zero);
  }

  @Test
  public void testIdentifiedEdgeIsFollowed() {
    // (theta 0 (+ theta 1))
    final PegNode.ThetaNode theta = PegNode.theta(zero.id);
    final PegNode plus = PegNode.opNode("+", theta.id, one.id);
    theta.setContinuation(plus.id);

    final Map<PegNode, Integer> map = new HashMap<>();
    theta.accept(v, map);
    assertOrder(map, theta, zero, plus, one);
  }

  public void assertOrder(Map<PegNode, Integer> map, PegNode...nodes) {
    int i = 0;
    for (PegNode n : nodes) {
      final Integer id = map.get(n);
      assertNotNull("Expected node " + n + " to be enumerated", id);
      assertEquals("Unexpected node index:", i++, map.get(n).intValue());
    }
  }


  public class PreOrderEnumeratorVisitor extends PegVisitor<Void, Map<PegNode, Integer>>  {
    protected int idx = 0;

    @Override
    protected void preVisit(PegNode.IntLit node, Map<PegNode, Integer> arg) {
      arg.put(node, idx++);
    }

    @Override
    protected void preVisit(PegNode.OpNode node, Map<PegNode, Integer> arg) {
      arg.put(node, idx++);
    }

    @Override
    protected void preVisit(PegNode.BoolLit node, Map<PegNode, Integer> arg) {
      arg.put(node, idx++);
    }

    @Override
    protected void preVisit(PegNode.PhiNode node, Map<PegNode, Integer> arg) {
      arg.put(node, idx++);
    }

    @Override
    protected void preVisit(PegNode.ThetaNode node, Map<PegNode, Integer> arg) {
      arg.put(node, idx++);
    }

    @Override
    protected void preVisit(PegNode.StringLit node, Map<PegNode, Integer> arg) {
      arg.put(node, idx++);
    }
  }
}