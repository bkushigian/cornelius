package serializer.peg.visitor;

import serializer.peg.PegNode;

public class PegVisitor<R, A> {
  public R visit(PegNode.OpNode node, A arg) {
    R r = null;
    for (final PegNode child : node.getChildrenNodes()) {
      if ((r = child.accept(this, arg)) != null) {
        return r;
      }
    }
    return r;
  }

  public R visit(PegNode.PhiNode node, A arg) {
    R r;
    if ((r = node.getGuard().accept(this, arg)) != null)
      return r;
    if ((r = node.getThen().accept(this, arg)) != null)
      return r;
    return node.getElse().accept(this, arg);
  }

  public R visit(PegNode.IntLit node, A arg) {
    return null;
  }

  public R visit(PegNode.BoolLit node, A arg) {
    return null;
  }

  public R visit(PegNode.StringLit node, A arg) {
    return null;
  }

  public R visit(PegNode.ThetaNode node, A arg) {
    R r;
    if ((r = node.getInitializer().accept(this, arg)) != null)
      return r;
    return node.getContinuation().accept(this, arg);
  }

}
