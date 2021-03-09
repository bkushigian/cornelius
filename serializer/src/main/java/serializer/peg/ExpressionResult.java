package serializer.peg;

/**
 * <p>An {@code ExpressionResult} tracks the result of visiting an expression. There are two
 * things we need to track: (1) the resulting PEG, and (2) the changes in state reflecting
 * the side effects of evaluating this expression.</p>
 * <p>For instance, if the expression is {@code x + foo()}, the peg would be of the form</p>
 *
 * <p><code>
 * (+ (param x)
 *    (invoke->peg
 *      (invoke (heap 0 unit)
 *        (param "this")
 *        "foo"
 *        (actuals))))
 * </code></p>
 *
 * <p>and the context would have to track the possibility that {@code foo()} threw an
 * exception, which would be done in the context.</p>
 */
public class ExpressionResult {
  public final PegNode peg;
  public final PegContext context;
  public ExpressionResult(final PegNode peg, final PegContext context) {
    this.peg = peg;
    this.context = context;
  }

  /**
   * Return this {@code ExpressionResult} with {@code this.context.heap} replaced by {@code heap}
   * @param heap
   * @return
   */
  public ExpressionResult withHeap(PegNode.Heap heap) {
    return new ExpressionResult(peg, context.withHeap(heap));
  }

  /**
   * Return this {@code ExpressionResult} with {@code this.context} replaced by {@code context}
   * @param context
   * @return
   */
  public ExpressionResult withContext(PegContext context) {
    return new ExpressionResult(peg, context);
  }

  /**
   * Return this {@code ExpressionResult} with the {@code this.peg} replaced by {@code peg}
   * @param peg
   * @return
   */
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

  /**
   * Return this {@code ExpressionResult} with a new exceptional condition added to the context
   * @param condition
   * @param exception
   * @return
   */
  public ExpressionResult withExceptionCondition(final PegNode condition, final PegNode exception) {
    return withContext(context.withExceptionCondition(condition, exception));
  }
}
