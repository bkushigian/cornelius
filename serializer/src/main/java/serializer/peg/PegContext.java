package serializer.peg;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PegContext {

    final private ImmutableMap<String, PegNode> localVariableLookup;
    final private Set<String> fieldNames;
    final PegNode.Heap heap;
    final ImmutableSet<PegNode> exitConditions;

    // XXX The following relies on there being a _unique_ return node in the AST
    PegNode returnNode = null;

    private PegContext(final ImmutableMap<String, PegNode> localVariableLookup,
                       final Set<String> fieldNames,
                       final PegNode.Heap heap,
                       final ImmutableSet<PegNode> exitConditions) {
        this.localVariableLookup = localVariableLookup;
        this.fieldNames = fieldNames;
        this.heap = heap;
        this.exitConditions = exitConditions;
    }

    /**
     * Combine two contexts, merging control flow.
     * @param c1 the context resulting from the then branch that executes if {@code guardId} is true
     * @param c2 the context resulting from the else branch that executes if {@code guardId} is false
     * @param guardId the id of the branching condition
     * @return combined contexts
     */
    public static PegContext combine(PegContext c1, PegContext c2, Integer guardId) {
        assert c1.fieldNames == c2.fieldNames;  // TODO: is this true? This should be true
        final ImmutableSet<String> domain = c1.localVariableLookup.keySet().stream().filter(c2.localVariableLookup::containsKey)
                .collect(Collectors.collectingAndThen(Collectors.toSet(), ImmutableSet::copyOf));

        final PegNode.Heap combinedHeap = PegNode.heap(
                PegNode.phi(guardId, c1.heap.state, c2.heap.state).id,
                PegNode.phi(guardId, c1.heap.status, c2.heap.status).id
        );

        final ImmutableSet<PegNode> combinedExitConditions = (new ImmutableSet.Builder<PegNode>())
                .addAll(c1.exitConditions)
                .addAll(c2.exitConditions).build();

        return initMap(
                domain,
                p -> PegNode.phi(guardId, c1.getLocalVar(p).id, c2.getLocalVar(p).id),
                c1.fieldNames,
                combinedHeap,
                combinedExitConditions);
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
      return isField(key) && !localVariableLookup.containsKey(key);
    }

    /**
     * Lookup a key in the context. This key can correspond to a {@code parameter} or a {@code field}
     * @param key the method parameter or field name to look up in this context
     * @return the associated {@code PegNode} if it exists, and PegNode.unit() otherwise.
     */
    public PegNode getLocalVar(String key) {
        if (localVariableLookup.containsKey(key)) {
            return localVariableLookup.get(key);
        }
        if (isUnshadowedField(key)) {
            // Todo: check for static fields/etc
            return PegNode.rd(PegNode.path(getLocalVar("this").id, key).id, heap.id);
        }
        return PegNode.unit();
    }

    /**
     * @param key lookup key to be associated with a value
     * @param val the value to be associated with the lookup key
     * @return new {@code PegContext} identical to this except for at {@code key} now maps to {@code val}
     * @throws IllegalArgumentException if key or val is null
     */
    public PegContext setLocalVar(final String key, final PegNode val) {
        if (key == null) throw new IllegalArgumentException("Cannot add null key to serializer.peg.PegContext");
        if (val == null) throw new IllegalArgumentException("Cannot add null val to serializer.peg.PegContext");

        final ImmutableMap.Builder<String, PegNode> b = new ImmutableMap.Builder<>();
        b.put(key, val);
        if (localVariableLookup.containsKey(key)) {
            for (ImmutableMap.Entry<String, PegNode> e : localVariableLookup.entrySet()) {
                if (! key.equals(e.getKey())) {
                    b.put(e);
                }
            }
        } else {
            b.putAll(localVariableLookup);
        }
        return new PegContext(b.build(), fieldNames, heap, exitConditions);
    }

    /**
     * A helper method wrapping {@code setLocalVar} to predicate assignments on appropriate checks against
     * exitConditions.
     * @param key variable name we are assigning to
     * @param val value we are assigning
     * @return context resulting from assignment
     */
    public PegContext performAssignLocalVar(final String key, final PegNode val) {
      if (exitConditions.isEmpty()) {
          return setLocalVar(key, val);
      }
      return setLocalVar(key, PegNode.phi(PegNode.exitConditions(exitConditions).id, getLocalVar(key).id, val.id));
    }

    /**
     * Update the heap
     * @param heap the new heap to be used in the new {@code PegContext}
     * @return a new {@code PegContext} identical to this one save for it's heap value, which is set to the passed
     *         in {@code heap}'s value
     */
    public PegContext withHeap(final PegNode.Heap heap) {
      return new PegContext(localVariableLookup, fieldNames, heap, exitConditions);
    }

    /**
     * Create a new PegContext identical to this one but with an added exit condition.
     * @param exitCondition the condition to add
     * @return the new PegContext with updated exitConditions
     */
    public PegContext withExitCondition(final PegNode exitCondition) {
      ImmutableSet.Builder<PegNode> builder = ImmutableSet.builder();
      builder.addAll(exitConditions);
      builder.add(exitCondition);
      final ImmutableSet<PegNode> exitConditions = builder.build();
      return new PegContext(localVariableLookup, fieldNames, heap, exitConditions);
    }

    /**
     * Update this context's exception status to reflect possible exceptional behavior
     * @param condition the boolean condition that implies exceptional behavior
     * @param exception the exception
     * @return
     */
    public PegContext withExceptionCondition(final PegNode condition, final PegNode exception) {
        PegNode.Heap newHeap;
        // Check for the case of `(phi (isunit? unit) thn els)` and transform to `thn`
        if (heap.status == PegNode.unit().id) {
            newHeap = heap.withStatus(PegNode.phi(condition.id, exception.id, PegNode.unit().id).id);
        }
        // Update the status to check if we already have an exception. If so, pass that exceptional status on.
        // Otherwise, mark the current status as {@code exception}
        // (phi (isunit? heap.status                            ;; If the status is unit, nothing thrown yet
        //               (phi condition.id exception.id unit)   ;; ...so check for exception
        //               heap.status)                           ;; Otherwise, use that status
        else {
            newHeap = heap.withStatus(
                    PegNode.phi(
                            PegNode.isunit(heap.status).id,
                            PegNode.phi(condition.id, exception.id, PegNode.unit().id).id,
                            heap.status).id
            );
        }
        return withExitCondition(condition).withHeap(newHeap);
    }

    /**
     * Initialize a map from a domain and a function on that domain
     * @param keys the keys that the new {@code PegContext} will operate over
     * @param f  function to combine peg nodes under a guard
     * @param fieldNames names of fields used by this method
     * @param heap the heap value to use in the resulting {@code PegContext}
     * @return a new context mapping all keys to values as specified by {@code f}
     */
    public static PegContext initMap(final Set<String> keys,
                                     final Function<String, PegNode> f,
                                     final Set<String> fieldNames,
                                     final PegNode.Heap heap,
                                     final ImmutableSet<PegNode> exitConditions)
    {

        final ImmutableMap.Builder<String, PegNode> builder = ImmutableMap.builderWithExpectedSize(keys.size());
        keys.forEach(k -> builder.put(k, f.apply(k)));
        return new PegContext(builder.build(), fieldNames, heap, exitConditions);
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
        return new PegContext(builder.build(), fieldNames, PegNode.initialHeap(), ImmutableSet.of());
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
