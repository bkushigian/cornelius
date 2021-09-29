package serializer.peg;

public class Pair<A, B> {
  public final A fst;
  public final B snd;

  /**
   * Create a new Pair tuple
   * @param first
   * @param second
   */
  public Pair(A first, B second) {
    fst = first;
    snd = second;
  }
}
