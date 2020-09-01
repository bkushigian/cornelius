package serializer.peg;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PegContext {
    final private ImmutableMap<String, PegNode> paramLookup;
    final private Set<String> fieldNames;
    final PegNode heap;
    static final PegContext empty = new PegContext();
    PegNode returnNode = null;

    private PegContext() {
        paramLookup = ImmutableMap.of();
        fieldNames = new HashSet<>();
        heap = PegNode.heap();
    }

    private PegContext(final PegContext ctx, final PegNode heap) {
        this.heap = heap;
        this.paramLookup = ctx.paramLookup;
        this.fieldNames = ctx.fieldNames;
    }

    private PegContext(final Set<String> keys, final Function<String, PegNode> f, final Set<String> fieldNames,
                       final PegNode heap) {
        final ImmutableMap.Builder<String, PegNode> builder = ImmutableMap.builderWithExpectedSize(keys.size());
        keys.forEach(k -> builder.put(k, f.apply(k)));
        paramLookup = builder.build();
        this.fieldNames = fieldNames;
        this.heap = heap;
    }

    private PegContext(ImmutableMap<String, PegNode> map, final Set<String> fieldNames, final PegNode heap) {
        this.fieldNames = fieldNames;
        this.paramLookup = map;
        this.heap = heap;
    }

    /**
     * Combine two contexts, say after an if-else branch merge.
     * @param c1 the first context
     * @param c2 the second context
     * @param f the combination function; should normally be something like:
     *  <pre>
     *   p -> p.fst.equals(p.snd) ?
     *     p.fst :
     *    PegNode.phi(guard.id, p.fst.id, p.snd.id)
     *  </pre>
     * @return the combined context
     */
    public static PegContext combine(PegContext c1, PegContext c2, Function<Pair<PegNode, PegNode>, PegNode> f) {
        final ImmutableSet<String> domain = c1.paramLookup.keySet().stream().filter(c2.paramLookup::containsKey)
                .collect(Collectors.collectingAndThen(Collectors.toSet(), ImmutableSet::copyOf));

        assert c1.fieldNames == c2.fieldNames;  // TODO: is this true? This should be true
        final PegNode heap = f.apply(new Pair<>(c1.heap, c2.heap));
        return PegContext.initMap(domain, k -> f.apply(new Pair<>(c1.get(k), c2.get(k))), c1.fieldNames, heap);
    }

    public static PegContext combine(PegContext c1, PegContext c2, Integer guardId) {
        return PegContext.combine(c1, c2, p -> p.fst.equals(p.snd) ? p.fst : PegNode.phi(guardId, p.fst.id, p.snd.id));

    }

    /**
     * @param key name of variable to look up
     * @return if there is a field in this class with the name {@code key}. NOTE that this doesn't account for
     * shadowing; for that, use {@code isUnshadowedField}
     */
    public boolean isField(final String key) {
        return fieldNames.contains(key);
    }

    public boolean isUnshadowedField(final String key) {
      return isField(key) && !paramLookup.containsKey(key);

    }

    /**
     * Lookup a key in the context. This key can correspond to a {@code parameter} or a {@code field}
     * @param key the method parameter or field name to look up in this context
     * @return the associated {@code PegNode}
     * @throws IllegalArgumentException if {@code key} is neither a method parameter or a field accessed by the method
     */
    public PegNode get(String key) {
        if (paramLookup.containsKey(key)) {
            return paramLookup.get(key);
        }
        if (fieldNames.contains(key)) {
            // Todo: check for static fields/etc
            return PegNode.rd(PegNode.path(get("this").id, key).id, heap.id);
        }
        throw new IllegalArgumentException("No such lookup item " + key);
    }

    /**
     * @param key lookup key to be associated with a value
     * @param val the value to be associated with the lookup key
     * @return new {@code PegContext} identical to this except for at {@code key} now maps to {@code val}
     * @throws IllegalArgumentException if key or val is null
     */
    public PegContext set(final String key, final PegNode val) {
        if (key == null) throw new IllegalArgumentException("Cannot add null key to serializer.peg.PegContext");
        if (val == null) throw new IllegalArgumentException("Cannot add null val to serializer.peg.PegContext");

        final ImmutableMap.Builder<String, PegNode> b = new ImmutableMap.Builder<>();
        b.put(key, val);
        if (paramLookup.containsKey(key)) {
            for (ImmutableMap.Entry<String, PegNode> e : paramLookup.entrySet()) {
                if (! key.equals(e.getKey())) {
                    b.put(e);
                }
            }
        } else {
            b.putAll(paramLookup);
        }
        return new PegContext(b.build(), fieldNames, heap);
    }

    /**
     * Update the heap
     * @param heap the new heap to be used in the new {@code PegContext}
     * @return a new {@code PegContext} identical to this one save for it's heap value, which is set to the passed
     *         in {@code heap}'s value
     */
    public PegContext withHeap(final PegNode heap) {
        return new PegContext(this, heap);
    }

    /**
     * Initialize a map from a domain and a function on that domain
     * @param keys the keys that the new {@code PegContext} will operate over
     * @param f  function to combine peg nodes under a guard
     * @param fieldNames names of fields used by this method
     * @param heap the heap value to use in the resulting {@code PegContext}
     * @return a new context mapping all keys to values as specified by {@code f}
     */
    public static PegContext initMap(final Set<String> keys, final Function<String, PegNode> f,
                                     final Set<String> fieldNames, final PegNode heap) {
        return new PegContext(keys, f, fieldNames, heap);
    }

    /**
     * Initialize a context with a set of parameters and fieldNames. This is how a new context should be created
     * @param fieldNames: the names of all fields accessed by this method. This is used for implicit {@code this}
     *                  dereferences, so it suffices to only include those field names that are referenced without a
     *                  prefix of {@code this}
     * @param params the list of parameter names. If the method is not static, this should include {@code this}.
     *
     * @return a {@code PegContext} representing the program state at the point of entry in the method. In
     * particular, each paramter name {@code name} will map to a {@code (var name)}.
     *
     */
    public static PegContext initWithParams(final Set<String> fieldNames, final List<String> params) {
        final ImmutableMap.Builder<String, PegNode> builder = ImmutableMap.builder();
        for (String param : params) {
            builder.put(param, PegNode.var(param));
        }
        return new PegContext(builder.build(), fieldNames, PegNode.initialHeap());
    }

    /**
     * @return a {@code method-result} node tracking the returned value and the updated heap
     */
    public Optional<PegNode> asPeg() {
        if (returnNode == null) {
            returnNode = PegNode.unit();
        }
        return Optional.ofNullable(PegNode.opNodeFromPegs("method-root", returnNode, heap));
    }

    public ExpressionResult exprResult(final PegNode peg) {
        return new ExpressionResult(peg, this);
    }
}
