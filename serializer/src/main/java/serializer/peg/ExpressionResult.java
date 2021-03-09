package serializer.peg;

public class ExpressionResult {
  public final PegNode peg;
  public final PegContext context;
  public ExpressionResult(final PegNode peg, final PegContext context) {
    this.peg = peg;
    this.context = context;
  }

  public ExpressionResult withHeap(PegNode.Heap heap) {
    return new ExpressionResult(peg, context.withHeap(heap));
  }

  public ExpressionResult withContext(PegContext context) {
    return new ExpressionResult(peg, context);
  }

  public ExpressionResult withPeg(PegNode peg) {
    return new ExpressionResult(peg, context);
  }

  /**
   * Return this {@code ExpressionResult} with a new exit condition added to the context
   * @param exitCondition
   * @return
   */
  public ExpressionResult withExitCondition(final PegNode exitCondition) {
    return withContext(context.withExitCondition(exitCondition));
  }
}
