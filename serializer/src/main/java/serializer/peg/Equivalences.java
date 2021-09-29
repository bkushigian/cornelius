package serializer.peg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class wraps a list of Id pairs as well as adding some extra sanity checking
 * to ensure that illegal states are never reached.
 *
 * This class ensures that no id is present in more than one equivalence, and that
 * an id is never registered as equivalent to itself
 */
public class Equivalences {
  /**
   * a list of equivalent id pairs we've come across
   */
  private List<Pair<Integer, Integer>> equivalences = new ArrayList<>();

  /**
   * A list of ids we've seen before.
   */
  private Set<Integer> alreadySeenIds = new HashSet<>();

  /**
   * Add an equivalence between ids {@code a} and {@code b}
   * @param id1 first id in equivalence
   * @param id2 second id in equivalence
   * @throws IllegalStateException  when {@code a.equals(b)} OR either a or b is already registered in an equivalence
   */
  public void addEquivalence(Integer id1, Integer id2) {
    if (alreadySeenIds.contains(id1) || alreadySeenIds.contains(id2)) {
      throw new IllegalStateException(String.format("Tried to add equivalence (%d, %d), but at least one element is " +
              "already in another equivalence", id1, id2));
    }

    if (id1.equals(id2)) {
      throw new IllegalStateException("Cannot register an id as equivalent to itself");
    }
    equivalences.add(new Pair<>(id1, id2));
  }

  /**
   * @return a copy of the backing equivalences list
   */
  public List<Pair<Integer, Integer>> getEquivalences() {
    return new ArrayList<>(equivalences);
  }
}
