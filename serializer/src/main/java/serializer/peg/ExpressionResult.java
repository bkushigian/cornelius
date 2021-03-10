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
   * Return this {@code ExpressionResult} with {@code this.context.heap} replaced by {@code heap}.
   * This does not modify the existing {@code ExpressionResult}.
   *
   * @param heap heap to replace {@code this.heap} with
   * @return the updated {@code ExpressionResult}
   */
  public ExpressionResult withHeap(PegNode.Heap heap) {
    return new ExpressionResult(peg, context.withHeap(heap));
  }

  /**
   * Return this {@code ExpressionResult} with {@code this.context} replaced by {@code context}
   * This does not modify the existing {@code ExpressionResult}.
   *
   * @param context the context to replace {@code this.context} with
   * @return the updated {@code ExpressionResult}
   */
  public ExpressionResult withContext(PegContext context) {
    return new ExpressionResult(peg, context);
  }

  /**
   * Return this {@code ExpressionResult} with the {@code this.peg} replaced by {@code peg}
   * This does not modify the existing {@code ExpressionResult}.
   *
   * @param peg the peg to replace {@code this.peg} with
   * @return the updated {@code ExpressionResult}
   */
  public ExpressionResult withPeg(PegNode peg) {
    return new ExpressionResult(peg, context);
  }

  /**
   * Return this {@code ExpressionResult} with a new exit condition added to the context
   * This does not modify the existing {@code ExpressionResult}.
   *
   * @param exitCondition the exit condition to be added to the context
   * @return the updated {@code ExpressionResult}
   */
  public ExpressionResult withExitCondition(final PegNode exitCondition) {
    return withContext(context.withExitCondition(exitCondition));
  }

  /**
   * Return this {@code ExpressionResult} with a new exceptional condition added to the context
   * This does not modify the existing {@code ExpressionResult}.
   *
   * @param condition the condition that triggers {@code exception}
   * @param exception the exception that is triggered when {@code condition} is met
   * @return the updated {@code ExpressionResult}
   */
  public ExpressionResult withExceptionCondition(final PegNode condition, final PegNode exception) {
    return withContext(context.withExceptionCondition(condition, exception));
  }

  /**
   * Combine two ExpressionResults based on guard
   * @param guard the condition determining if the first or second ExpressionResult is used
   * @param thn the then-branch ExpressionResult to use
   * @param els the else-branch ExpressionResult to use
   * @return a new ExpressionResult where context is combined based on guard, and peg is a phi node:
   *       {@code (phi guard thn.peg els.peg)}
   */
  public static ExpressionResult combine(final PegNode guard, final ExpressionResult thn, final ExpressionResult els) {
    return new ExpressionResult(PegNode.phi(guard.id, thn.peg.id, els.peg.id),
            PegContext.combine(thn.context, els.context, guard.id));
  }
}
