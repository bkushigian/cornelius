package serializer.peg;

import serializer.peg.visitor.PegVisitor;

import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class StructuralEquivalence {

  public static Optional<NonEquivalenceWitness> checkEquivalence(Integer expected, Integer actual) {
    PegNode expectedNode = PegNode.idLookup(expected).orElseThrow(IllegalArgumentException::new);
    PegNode actualNode = PegNode.idLookup(actual).orElseThrow(IllegalArgumentException::new);
    StructuralEquivalenceVisitor sev = new StructuralEquivalenceVisitor();
    return expectedNode.accept(sev, actualNode);
  }

  static abstract class NonEquivalenceWitness{};

  static class EquivalenceError extends NonEquivalenceWitness {
    PegNode expected;
    PegNode actual;

    EquivalenceError(PegNode expected, PegNode actual) {
      this.expected = expected;
      this.actual = actual;
    }

    @Override
    public String toString() {
      return String.format("Equivalence Error: expected %s but saw %s", expected.toDerefString(), actual.toDerefString());
    }
  }

  static class BijectionError extends NonEquivalenceWitness {
    PegNode reassigned;
    PegNode other;
    PegNode assignedTo;

    BijectionError(PegNode reassigned, PegNode other, PegNode assignedTo) {
      this.reassigned = reassigned;
      this.other = other;
      this.assignedTo = assignedTo;
    }

    @Override
    public String toString() {
      return String.format("Bijection Error: Compared %s and %s when %s already associated with %s",
                           reassigned.toDerefString(), other.toDerefString(), reassigned.toDerefString(),
                           assignedTo.toDerefString());
    }
  }

  static class AssignmentError extends NonEquivalenceWitness {
    PegNode.ThetaNode unassigned;

    AssignmentError(PegNode.ThetaNode unassigned) {
      this.unassigned = unassigned;
    }

    @Override
    public String toString() {
      return "Assignment Error: the following ThetaNode was unassigned - " + unassigned.toDerefString();
    }
  }
  
  /**
   * Visitor used by StructuralBijection to generate a BijectionError
   */
  static class StructuralEquivalenceVisitor extends PegVisitor<Optional<NonEquivalenceWitness>, PegNode> {
    private Map<Integer, Integer> bijection = new HashMap<>();

    private PegNode safeIdLookup(int id) {
      return PegNode.idLookup(id).orElseThrow(IllegalStateException::new);
    }

    // if the pegs are associated or we have a bijection error, return an optional of the return val
    // otherwise add pegs to the bijection and return empty
    private Optional<Optional<NonEquivalenceWitness>> checkBijection(final PegNode node1, final PegNode node2) {
      if (bijection.containsKey(node1.id) && bijection.containsKey(node2.id)) {
        if (!bijection.get(node1.id).equals(node2.id)) {
          return Optional.of(Optional.of(new BijectionError(node1, node2, safeIdLookup(bijection.get(node1.id)))));
        } else {
          return Optional.of(Optional.empty());
        }
      } else if (bijection.containsKey(node1.id)) {
        return Optional.of(Optional.of(new BijectionError(node1, node2, safeIdLookup(bijection.get(node1.id)))));
      } else if (bijection.containsKey(node2.id)) {
        return Optional.of(Optional.of(new BijectionError(node2, node1, safeIdLookup(bijection.get(node2.id)))));
      } else {
        bijection.put(node1.id, node2.id);
        bijection.put(node2.id, node1.id);
        return Optional.empty();
      }
    }


    public Optional<NonEquivalenceWitness> visit(final PegNode.IntLit node, final PegNode other) {
      if (node.id == other.id) return Optional.empty();
      // due to deduplication, IntLits are only equivalent if they have the same id
      else return Optional.of(new EquivalenceError(node, other));
    }

    public Optional<NonEquivalenceWitness> visit(final PegNode.BoolLit node, final PegNode other) {
      if (node.id == other.id) return Optional.empty();
      // due to deduplication, BoolLits are only equivalent if they have the same id
      else return Optional.of(new EquivalenceError(node, other));
    }

    public Optional<NonEquivalenceWitness> visit(final PegNode.StringLit node, final PegNode other) {
      if (node.id == other.id) return Optional.empty();
      // due to deduplication, StringLits are only equivalent if they have the same id
      else return Optional.of(new EquivalenceError(node, other));
    }
  
    public Optional<NonEquivalenceWitness> visit(final PegNode.OpNode node, final PegNode other) {
      if (node.id == other.id) return Optional.empty();
      Optional<Optional<NonEquivalenceWitness>> bijOpt = checkBijection(node, other);
      if (bijOpt.isPresent()) return bijOpt.get();

      // ensure other is an OpNode
      Optional<PegNode.OpNode> otherOpt = other.asOpNode();
      if (!otherOpt.isPresent()) return Optional.of(new EquivalenceError(node, other));
      PegNode.OpNode otherOpNode = otherOpt.get();

      // ensure it is the same type of OpNode
      if (!node.op.equals(otherOpNode.op)) return Optional.of(new EquivalenceError(node, other));
      
      // visit children and check they are equivalent
      List<PegNode> children = node.getChildrenNodes();
      List<PegNode> otherChildren = otherOpNode.getChildrenNodes();
      if (children.size() != otherChildren.size()) return Optional.of(new EquivalenceError(node, other));
      for (int i = 0; i < children.size(); i++) {
        Optional<NonEquivalenceWitness> result = children.get(i).accept(this, otherChildren.get(i));
        if (result.isPresent()) return result;
      }

      return Optional.empty();
    }

    public Optional<NonEquivalenceWitness> visit(final PegNode.PhiNode node, final PegNode other) {
      if (node.id == other.id) return Optional.empty();
      Optional<Optional<NonEquivalenceWitness>> bijOpt = checkBijection(node, other);
      if (bijOpt.isPresent()) return bijOpt.get();

      // ensure other is a PhiNode
      Optional<PegNode.PhiNode> otherOpt = other.asPhiNode();
      if (!otherOpt.isPresent()) return Optional.of(new EquivalenceError(node, other));
      PegNode.PhiNode otherPhiNode = otherOpt.get();
      
      // visit children and check they are equivalent
      Optional<NonEquivalenceWitness> result = node.getGuard().accept(this, otherPhiNode.getGuard());
      if (result.isPresent()) return result;
      result = node.getThen().accept(this, otherPhiNode.getThen());
      if (result.isPresent()) return result;
      result = node.getElse().accept(this, otherPhiNode.getElse());
      if (result.isPresent()) return result;

      return Optional.empty();
    }

    public Optional<NonEquivalenceWitness> visit(final PegNode.ThetaNode node, final PegNode other) {
      Optional<Optional<NonEquivalenceWitness>> bijOpt = checkBijection(node, other);
      if (bijOpt.isPresent()) return bijOpt.get();

      // ensure other is a Theta
      Optional<PegNode.ThetaNode> otherOpt = other.asThetaNode();
      if (!otherOpt.isPresent()) return Optional.of(new EquivalenceError(node, other));
      PegNode.ThetaNode otherThetaNode = otherOpt.get();

      // check thetas are assigned
      if (!node.getContinuation().isPresent()) return Optional.of(new AssignmentError(node));
      if (!otherThetaNode.getContinuation().isPresent()) return Optional.of(new AssignmentError(otherThetaNode));
      
      // visit children and check they are equivalent
      Optional<NonEquivalenceWitness> result = node.getInitializer().accept(this, otherThetaNode.getInitializer());
      if (result.isPresent()) return result;
      result = node.getContinuation().get().accept(this, otherThetaNode.getContinuation().get());
      if (result.isPresent()) return result;

      return Optional.empty();
    }

    public Optional<NonEquivalenceWitness> visit(final PegNode.Heap node, final PegNode other) {
      if (node.id == other.id) return Optional.empty();
      Optional<Optional<NonEquivalenceWitness>> bijOpt = checkBijection(node, other);
      if (bijOpt.isPresent()) return bijOpt.get();

      // ensure other is a Heap
      Optional<PegNode.Heap> otherOpt = other.asHeap();
      if (!otherOpt.isPresent()) return Optional.of(new EquivalenceError(node, other));
      PegNode.Heap otherHeap = otherOpt.get();
      
      // visit children and check they are equivalent
      Optional<NonEquivalenceWitness> result = node.getState().accept(this, otherHeap.getState());
      if (result.isPresent()) return result;
      result = node.getStatus().accept(this, otherHeap.getStatus());
      if (result.isPresent()) return result;

      return Optional.empty();
    }
  }
}
