package serializer.peg.visitor;

import serializer.peg.PegNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class PegVisitor<R, A> {
  protected Map<PegNode, R> table = new HashMap<>();

  public R visit(final PegNode.OpNode node, final A arg) {
    if (table.containsKey(node)) {
      return table.get(node);
    }
    preVisit(node, arg);
    for (final PegNode child : node.getChildrenNodes()) {
      child.accept(this, arg);
    }

    final List<R> children = node.getChildrenNodes().stream().map(table::get).collect(Collectors.toList());

    table.put(node, combine(node, arg, children));
    return table.get(node);
  }

  public R visit(final PegNode.ThetaNode node, final A arg) {
    if (table.containsKey(node)) {
      return table.get(node);
    }
    preVisit(node, arg);
    final R init = node.getInitializer().accept(this, arg);
    final R cont = node.getContinuation().accept(this, arg);
    table.put(node, combine(node, arg, init, cont));
    return table.get(node);
  }

  public R visit(final PegNode.PhiNode node, final A arg) {
    if (table.containsKey(node)) {
      return table.get(node);
    }
    preVisit(node, arg);
    final R guard = node.getGuard().accept(this, arg);
    final R thn = node.getThen().accept(this, arg);
    final R els = node.getElse().accept(this, arg);
    table.put(node, combine(node, arg, guard, thn, els));
    return table.get(node);
  }

  public R visit(final PegNode.IntLit node, final A arg) {
    if (table.containsKey(node)) {
      return table.get(node);
    }
    preVisit(node, arg);

    table.put(node, combine(node, arg));
    return table.get(node);
  }

  public R visit(final PegNode.BoolLit node, final A arg) {
    if (table.containsKey(node)) {
      return table.get(node);
    }
    preVisit(node, arg);

    table.put(node, combine(node, arg));
    return table.get(node);
  }

  public R visit(final PegNode.StringLit node, final A arg) {
    if (table.containsKey(node)) {
      return table.get(node);
    }
    preVisit(node, arg);

    table.put(node, combine(node, arg));
    return table.get(node);
  }

  public R visit(final PegNode.BlankNode node, final A arg) {
    if (table.containsKey(node)) {
      return table.get(node);
    }
    preVisit(node, arg);
    final Optional<PegNode> identNode = node.getIdentifiedNode();
    final R combined = identNode.isPresent() ? combine(node, arg, identNode.get()) : combine(node, arg);
    table.put(node, combined);
    return combined;
  }

  protected void preVisit(final PegNode.OpNode node, final A arg) {}
  protected void preVisit(final PegNode.ThetaNode node, final A arg) {}
  protected void preVisit(final PegNode.PhiNode node, final A arg) {}
  protected void preVisit(final PegNode.BlankNode node, final A arg) {}
  protected void preVisit(final PegNode.IntLit node, final A arg) {}
  protected void preVisit(final PegNode.BoolLit node, final A arg) {}
  protected void preVisit(final PegNode.StringLit node, final A arg) {}

  protected R combine(final PegNode.OpNode node, final A arg, final List<R> children) {
    return null;
  }

  protected R combine(final PegNode.ThetaNode node, final A arg, final R init, final R continuation) {
    return null;
  }

  protected R combine(final PegNode.PhiNode node, final A arg, final R guard, final R thn, final R els) {
    return null;
  }

  protected R combine(final PegNode.BlankNode node, final A arg) {
    return null;
  }

  protected R combine(final PegNode.BlankNode node, final A arg, final PegNode identified) {
    return null;
  }

  protected R combine(final PegNode.IntLit node, final A arg) {
    return null;
  }

  protected R combine(final PegNode.BoolLit node, final A arg) {
    return null;
  }

  protected R combine(final PegNode.StringLit node, final A arg) {
    return null;
  }
}
