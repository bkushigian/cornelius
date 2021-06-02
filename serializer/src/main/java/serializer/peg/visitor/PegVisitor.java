package serializer.peg.visitor;

import serializer.peg.PegNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>The {@code PegVisitor} is a <emph>memoizing</emph> visitor pattern. {@code PegNodes} differ from
 * traditional ASTs in two ways that we need to account for.</p>
 * <ol>
 *   <li>
 *     <strong>Deduplication:</strong> {@code PegNodes} are deduplicated: lexically identical nodes
 *     are represented by the same backing data structure. For instance, if there are two copies of
 *     the node {@code a} in a program, only a single {@code PegNode} will be created to represent
 *     all copies of {@code a} in the program.
 *    </li>
 *    <li>
 *      <strong>Cycles:</strong> {@code PegNode.ThetaNode}s allow for cycles through their
 *      identified node (obtainable from {@code thetaNode.getIdentifiedNode()}.
 *    </li>
 * </ol>
 *
 * <p> For many tasks, revisiting a deduplicated node is a waste of resources. This implementation
 * handles this by memoizing results in the protected {@link #table} field.</p>
 *
 * <p>The second problem is a little subtler. On the one hand we don't want infinite loops, which would
 * suggest not traversing the edge from a {@code theta} node to it's identified node (this is where a cycle
 * will form). On the other hand, sometimes a {@code theta} node will be reachable from the root of
 * the {@code PegNode} DAG but the identified node will <emph>not</emph> be reachable from the root
 * unless we traverse the theta's edge to an identified node. I'll cover how to solve this below.
 * </p>
 *
 * This implementation provides two basic ways to customize the visit:
 * <ol>
 *   <li>
 *     <strong>Overriding {@code preVisit}:</strong> this is called by a node's {@code visit} method
 *     (for instance, see {@link #visit(PegNode.OpNode, Object)}) <emph>after</emph> the memoization
 *     check has been done but <emph>before</emph> the node's children have been visited.
 *     This method could be use for, say, enumerating nodes in a pre-order, which would be difficult to
 *     do otherwise.
 *   </li>
 *
 *   <li>
 *     <strong>Overriding {@code combine}:</strong> {@code combine} is called by a node's {@code visit}
 *     method <empth>after</empth> the node's children have been visited, and should combine the results
 *     of the children into a new result. The {@code} visit pattern will take care of memoizing
 *     the result of {@code combine} for later use: all that {@code combine} needs to do is create
 *     the desired result from visiting the node.
 *   </li>
 *
 * </ol>
 *
 * There is a special case visiting {@code PegNode.ThetaNodes}. After {@code combine} has been called on
 * the theta node, we try to visit
 * @param <R> return type returned from each visit method
 * @param <A> argument type that is passed in to each visit method
 */
public class PegVisitor<R, A> {
  /**
   * The memoization table used to store results of visiting a node.
   */
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

  public R visit(final PegNode.ThetaNode node, final A arg) {
    if (table.containsKey(node)) {
      return table.get(node);
    }
    preVisit(node, arg);
    final R init = node.getInitializer().accept(this, arg);
    final Optional<PegNode> identNode = node.getIdentifiedNode();
    final R combined = identNode.isPresent() ? combine(node, arg, init, identNode.get()) : combine(node, arg, init);
    table.put(node, combined);
    identNode.ifPresent(pegNode -> pegNode.accept(this, arg));
    return combined;
  }

  protected void preVisit(final PegNode.OpNode node, final A arg) {}
  protected void preVisit(final PegNode.ThetaNode node, final A arg) {}
  protected void preVisit(final PegNode.PhiNode node, final A arg) {}
  protected void preVisit(final PegNode.IntLit node, final A arg) {}
  protected void preVisit(final PegNode.BoolLit node, final A arg) {}
  protected void preVisit(final PegNode.StringLit node, final A arg) {}

  protected R combine(final PegNode.OpNode node, final A arg, final List<R> children) {
    return null;
  }

  protected R combine(final PegNode.PhiNode node, final A arg, final R guard, final R thn, final R els) {
    return null;
  }

  protected R combine(final PegNode.ThetaNode node, final A arg, final R init) {
    return null;
  }

  protected R combine(final PegNode.ThetaNode node, final A arg, final R init, final PegNode identified) {
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
