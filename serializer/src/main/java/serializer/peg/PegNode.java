package serializer.peg;

import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.google.common.collect.ImmutableSet;

import java.util.*;
import java.util.stream.Collectors;

public abstract class PegNode {

    // TODO: make not static
    public static Map<Integer, PegNode> getIdLookup() {
        return new HashMap<>(idLookup);
    }

    public static Optional<PegNode> idLookup(final Integer id) {
        return Optional.ofNullable(idLookup.get(id));
    }

    public static void clearIdLookupTable() {
        idLookup.clear();
    }

    /**
     * Clear state by resetting id generation to 0 and clearing lookup tables.
     */
    public static void clear() {
        idLookup.clear();
        litLookup.clear();
        symbolLookup.clear();
        _id = 0;
    }

    public String toDerefString() {
        return toString();
    }

    static int _id = 0;

    protected final List<Integer> children;
    public final int id;

    PegNode(Integer...children) {
        this.id = _id++;
        for (Integer child: children) {
            if (child >= id) {
                throw new IllegalStateException(String.format("PegNode with id %d has child with id %d: children must have ids that are strictly less than that of their parents", id, child));
            }
        }
        idLookup.put(id, this);
        this.children = Arrays.asList(children);
    }

    public boolean isConst() {
        return false;
    }

    public boolean getOpNode() {
        return false;
    }

    public List<Integer> children() {
        return children;
    }

    public Optional<Integer> asInteger() {
        return Optional.empty();
    }

    public Optional<Boolean> asBoolean() {
        return Optional.empty();
    }

    public Optional<String> asString() {
        return Optional.empty();
    }

    public Optional<Heap> asHeap() {
            return Optional.empty();
    }

    public boolean isHeap() {
        return false;
    }

    public boolean isIntLit() {
        return false;
    }

    public boolean isBoolLit() {
        return false;
    }

    public boolean isStringLit() {
        return false;
    }

    public boolean isBlankNode() {
        return false;
    }

    public boolean isOpNode() {
        return false;
    }

    public boolean isPhiNode() {
        return false;
    }

    public boolean isThetaNode() {
        return false;
    }

    public Optional<PhiNode> asPhiNode() {
        return Optional.empty();
    }

    public Optional<ThetaNode> asThetaNode() {
        return Optional.empty();
    }

    public Optional<OpNode> asOpNode() {
        return Optional.empty();
    }

    public ExpressionResult exprResult(final PegContext context) {
        return new ExpressionResult(this, context);
    }

    public final static class IntLit extends PegNode {
        public final int value;
        private IntLit(int value) {
            this.value = value;
            idLookup.put(this.id, this);
            litLookup.put(value, this);
        }

        @Override
        public boolean isIntLit() {
            return true;
        }

        @Override
        public Optional<Integer> asInteger() {
            return Optional.of(value);
        }

        @Override
        public boolean isConst() {
            return true;
        }

        @Override
        public String toString() {
            return String.format("%d", value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            IntLit intLit = (IntLit) o;
            return value == intLit.value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

    }

    public final static class BoolLit extends PegNode {
        public final boolean value;
        private BoolLit(boolean value) {
            this.value = value;
            idLookup.put(this.id, this);
            litLookup.put(value, this);
        }

        @Override
        public boolean isBoolLit() {
            return true;
        }

        @Override
        public Optional<Boolean> asBoolean() {
            return Optional.of(value);
        }

        @Override
        public boolean isConst() {
            return true;
        }

        @Override
        public String toString() {
            return String.format("%b", value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BoolLit boolLit = (BoolLit) o;
            return value == boolLit.value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    public final static class StringLit extends PegNode {
        public final String value;
        private StringLit(String value) {
            this.value = value;
            idLookup.put(this.id, this);
            litLookup.put(value, this);
        }

        @Override
        public boolean isStringLit() {
            return true;
        }

        @Override
        public Optional<String> asString() {
            return Optional.of(value);
        }

        @Override
        public boolean isConst() {
            return true;
        }

        @Override
        public String toString() {
            return String.format("\"%s\"", value);
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof StringLit)) return false;
            StringLit stringLit = (StringLit) o;
            return value.equals(stringLit.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    public static class BlankNode extends PegNode {
        private BlankNode() {
            idLookup.put(id, this);
        }

        @Override
        public boolean isBlankNode() {
            return true;
        }
    }

    public static class OpNode extends PegNode {
        public final String op;

        private OpNode(String op, Integer...children) {
            super(children);
            this.op = op;
            idLookup.put(id, this);
            symbolLookup.computeIfAbsent(op, x -> new HashMap<>())
                        .put(this.children, this);
        }

        public List<PegNode> getChildrenNodes() {
            return children.stream().map(idLookup::get).collect(Collectors.toList());
        }

        @Override
        public boolean isOpNode() {
            return true;
        }

        @Override
        public String toDerefString() {
            if (children.isEmpty()) {
                return op;
            }
            final StringBuilder sb = new StringBuilder("(");
            sb.append(op);
            sb.append(' ');
            boolean added = false;
            for (Integer child : children) {
                if (added) {
                    sb.append(' ');
                }
                added = true;
                final PegNode p = idLookup.get(child);
                if (p == null) {
                    throw new IllegalStateException("OpNode " + op + " child index " + child + " not present");
                }
                sb.append(p.toDerefString());
            }

            return sb.append(")").toString();
        }

        @Override
        public boolean getOpNode() {
            return true;
        }

        @Override
        public String toString() {
            if (children.isEmpty())  {
                return op;
            }
            final StringBuilder sb = new StringBuilder();
            sb.append('(');
            sb.append(op);
            for (Integer cid : children) {
                sb.append(' ');
                sb.append(cid);
            }
            sb.append(')');
            return sb.toString();
        }

        @Override
        public Optional<OpNode> asOpNode() {
            return Optional.of(this);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OpNode opNode = (OpNode) o;
            return this.op.equals(opNode.op) && children.equals(opNode.children);
        }

        @Override
        public int hashCode() {
            return Objects.hash(op, children);
        }

    }

    public static class PhiNode extends OpNode {
        public final Integer guard;
        public final Integer thn;
        public final Integer els;
        private PhiNode(final Integer guard, final Integer thn, final Integer els) {
            super("phi", guard, thn, els);
            this.guard = guard;
            this.thn = thn;
            this.els = els;
        }

        @Override
        public Optional<PhiNode> asPhiNode() {
            return Optional.of(this);
        }

        @Override
        public boolean isPhiNode() {
            return true;
        }
    }

    public static class ThetaNode extends OpNode {
        public final Integer init;
        public final Integer next;
        private ThetaNode(final Integer init, final Integer next) {
            super("theta", init, next);
            this.init = init;
            this.next = next;
        }

        @Override
        public Optional<ThetaNode> asThetaNode() {
            return Optional.of(this);
        }

        @Override
        public boolean isThetaNode() {
            return true;
        }
    }

    public static class Heap extends OpNode {
        /**
         * The heap state's id
         */
        public final Integer state;
        /**
         * The heap status's id
         */
        public final Integer status;

        /**
         * Create a new Heap node
         * @param state the heap state's id
         * @param status the exception status's id
         */
        private Heap(final Integer state, final Integer status) {
            super("heap", state, status);
            this.state = state;
            this.status = status;
        }

        @Override
        public boolean isHeap() {
            return true;
        }

        @Override
        public Optional<Heap> asHeap() {
            return Optional.of(this);
        }

        public Heap withState(final Integer state) {
            return heap(state, status);
        }

        public Heap withStatus(final Integer status) {
            return heap(state, status);
        }
    }

    /**
     * lookup the PEG node from an id
     */
    private static Map<Integer, PegNode> idLookup = new HashMap<>();

    /**
     * lookup the PEG node from a symbol and a list of children id.
     *
     * WARNING: This is sketchy since I'm using a {@code List<Integer>} as a key,
     * and this is mutable. This shouldn't be a problem since I'm never updating
     * children (which are the hashes), but at some point I'll want to fix this
     *
     * TODO: Fix above warning
     */
    private static Map<String, Map<List<Integer>, PegNode>> symbolLookup = new HashMap<>();

    private static Map<Object, PegNode> litLookup = new HashMap<>();

    /**
     * Get an OpNode for sym being applied to children. This creates a new
     * OpNode if needed (i.e., if one with the same sym and children doesn't
     * already exist) and adds it's id to the idLookup table and adds its
     * symbol to the symbolLookup table.
     * @param sym operator symbol
     * @param children child PEG ids
     * @return a peg node representing (sym *children)
     */
    public static PegNode opNode(String sym, Integer...children) {
        final List<Integer> childs = Arrays.asList(children);
        if (!symbolLookup.containsKey(sym) || !symbolLookup.get(sym).containsKey(childs)) {
            return new OpNode(sym, children);
        }
        return symbolLookup.get(sym).get(childs);
    }

    public static PegNode opNodeFromPegs(String sym, PegNode...children) {
        return opNode(sym, Arrays.stream(children).map(x -> x.id).toArray(Integer[]::new));
    }

    public static PegNode intLit(int n) {
        if (!litLookup.containsKey(n)) {
            return new IntLit(n);
        }
        return litLookup.get(n);
    }

    public static PegNode boolLit(boolean b) {
        if (!litLookup.containsKey(b)) {
            return new BoolLit(b);
        }
        return litLookup.get(b);
    }

    public static PegNode stringLit(String s) {
        if (!litLookup.containsKey(s)) {
            return new StringLit(s);
        }
        return litLookup.get(s);
    }

    public static PegNode blank() {
        return new BlankNode();
    }

    public static PegNode unit() {
        return opNode("unit");
    }

    public static PegNode phi(Integer guard, Integer then, Integer els) {
      return opNode("phi", guard, then, els);
    }

    public static ThetaNode theta(Integer init, Integer next) {
      final String sym = "theta";
      final List<Integer> childs = new ArrayList<>(3);
      childs.add(init);
      childs.add(next);
      if (!symbolLookup.containsKey(sym) || !symbolLookup.get(sym).containsKey(childs)) {
          return new ThetaNode(init, next);
      }
      final PegNode node = symbolLookup.get(sym).get(childs);
      return node.asThetaNode().orElseThrow(() -> new IllegalStateException(
              String.format("Unexpected value cached for sym=\"theta\", children=[%d, %d];" +
                      " expected a PegNode.ThetaNode but found %s",
                      init, next, symbolLookup.get(sym).get(childs).toDerefString())));
    }

    public static PegNode var(String name) {
        return opNode("var", opNode(name).id);
    }

    public static PegNode derefs(String derefs) {
        return opNode("derefs", opNode(derefs).id);
    }

    public static PegNode path(Integer base, Integer derefs) {
        return opNode("path", base, derefs);
    }

    public static PegNode path(Integer base, String path) {
        return path(base, derefs(path).id);
    }

    public static PegNode rd(Integer path, Integer heap) {
        return opNode("rd", path, heap);
    }

    public static PegNode wr(Integer path, Integer val, Integer heap) {
        return opNode("wr", path, val, heap);
    }

    public static PegNode invoke(Integer heap, Integer receiver, String method, Integer actuals) {
        return opNode("invoke", heap, receiver, opNode(method).id, actuals);
    }

    public static PegNode actuals(Integer...actuals) {
        return opNode("actuals", actuals);
    }

    public static PegNode invokeToPeg(Integer invocation) {
        return opNode("invoke->peg", invocation);
    }

    public static PegNode invocationToHeapState(Integer invocation) {
        return opNode("invoke->heap-state", invocation);
    }

    public static PegNode invocationToExceptionStatus(Integer invocation) {
        return opNode("invoke->exception-status", invocation);
    }

    public static PegNode invocationThrew(Integer invocation) {
        return opNode("invocation-threw?", invocation);
    }

    public static Heap projectHeap(Integer invocation) {
        return heap(invocationToHeapState(invocation).id, invocationToExceptionStatus(invocation).id);
    }

    public static PegNode newObject(final String type, final Integer actuals, final Integer heap) {
        return opNode("new", stringLit(type).id, actuals, heap);
    }

    public static PegNode pass(Integer condition) {
        return opNode("pass", condition);
    }

    public static PegNode eval(Integer theta, Integer pass) {
        return opNode("eval", theta, pass);
    }

    /**
     * Get a heap node with {@code state} and {@code status} arguments, creating and caching one if one doesn't
     * already exist.
     * @param state
     * @param status
     * @return t
     * @throws IllegalStateException if the cached node is not a {@code PegNode.Heap}
     * @throws NullPointerException if either argument is {@code null}
     */
     public static Heap heap(Integer state, Integer status) {
         if (state == null || status == null) throw new IllegalStateException("Null heap-state or heap-status");
         final String sym = "heap";
         final List<Integer> childs = new ArrayList<>(2);
         childs.add(state);
         childs.add(status);
         if (!symbolLookup.containsKey(sym) || !symbolLookup.get(sym).containsKey(childs)) {
             return new Heap(state, status);
         }
         final PegNode node = symbolLookup.get(sym).get(childs);
         return node.asHeap().orElseThrow(() -> new IllegalStateException(
                 String.format("Unexpected value cached for sym=\"heap\", children=[%d, %d]; expected a PegNode.Heap " +
                         "but found %s", state, status, symbolLookup.get(sym).get(childs).toDerefString())));
     }

    /**
     * Get the initial heap, indexed at 0, to represent the heap coming into a method.
     * This should be the same for all methods since we are assuming that all methods are
     * executed with the same heap environment.
     *
     * NOTE: This will have to change for inlining; I'll mark this as a TODO
     * @return the initial heap {@code PegNode}
     */
    public static PegNode.Heap initialHeap() {
      return heap(intLit(0).id, unit().id);
    }

    public static Heap wrHeap(Integer path, Integer val, Heap heap) {
         return heap.withState(wr(path, val, heap.id).id);
    }

    public static PegNode nullLit() {
        return opNode("null");
    }

    public static PegNode isnull(Integer valId) {
        return opNode("isnull?", valId);
    }

    public static PegNode isunit(Integer valId) {
        return opNode("isunit?", valId);
    }

    public static PegNode exception(final String name) {
        return opNode(name);
    }

    public static PegNode returnNode(final Integer pegId, final Integer heapId) {
        return opNode("return-node", pegId, heapId);
    }

    /**
     * @param objId the object to be cast
     * @param typeId the type to be cast to
     * @return A PEG representing if a cast is legal or not.
     */
    public static PegNode canCast(final Integer objId, final Integer typeId) {
        return opNode("can-cast?", objId, typeId);
    }

    /**
     * Return a node representing a type name, such as "java.lang.Object"
     * @param name type name to embed in a PEG node
     * @return a PEG node representing that type name
     */
    public static PegNode typeName(final String name) {
        return opNode("type-name", stringLit(name).id);
    }

    /**
     * @param objId id of the object to be cast
     * @param typeId type to cast the object to
     * @return
     */
    public static PegNode cast(final Integer objId, final Integer typeId) {
        return opNode("cast", objId, typeId);
    }

    public static PegNode exitConditions(Collection<PegNode> conditions) {
        for (PegNode c : new HashSet<>(conditions)) {
            if (c == null) {
                throw new IllegalStateException("Found null condition");
            }
        }
        final List<Integer> childs = new ArrayList<>(conditions.size());
        childs.addAll(conditions.stream().map(c -> c.id).collect(Collectors.toList()));
        childs.sort(null);

        if (childs.isEmpty()) return boolLit(false);
        if (childs.size() == 1) return idLookup(childs.get(0)).orElseThrow(IllegalStateException::new);

        Integer id = childs.get(0);
        childs.remove(0);
        for (Integer childId : childs) {
            id = opNode("||", id, childId).id;
        }
        return idLookup(id).orElseThrow(IllegalStateException::new);
    }

    public static void replace(Integer blank, Integer value) {
        // maybe add arg checks later?
        idLookup.put(blank, idLookup.get(value));
    }
}
