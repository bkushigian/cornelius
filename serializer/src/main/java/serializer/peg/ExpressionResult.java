package serializer.peg;

public class ExpressionResult {
  public final PegNode peg;
  public final PegContext context;
  public ExpressionResult(final PegNode peg, final PegContext context) {
    this.peg = peg;
    this.context = context;
  }
}
