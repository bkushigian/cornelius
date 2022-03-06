package serializer.peg;

import serializer.peg.visitor.PegVisitor;

import java.util.*;
import java.util.stream.Collectors;

public abstract class PegNode {

    public abstract <R, A> R accept(PegVisitor<R, A> visitor, A arg);

    /**
     * @return a copy of the map mapping peg ids to their corresponding {@code PegNode}s
     *
     * TODO: make not static
     */
    public static Map<Integer, PegNode> getIdLookup() {
        return new HashMap<>(idLookup);
    }

    /**
     * Get a list of all node equivalences
     * @return list of node equivalences
     */
    public static List<Pair<Integer, Integer>> getNodeEquivalences() {
        return equivalences.getEquivalences();
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
        equivalences = new Equivalences();
        _id = 0;
        ThetaNode.BlankNode._blankId = 0;
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

    public Optional<Long> asLong() {
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

    public boolean isOpNode() {
        return false;
    }

    public boolean isPhiNode() {
        return false;
    }

    public boolean isThetaNode() {
        return false;
    }

    public Optional<OpNode> asOpNode() {
        return Optional.empty();
    }

    public Optional<PhiNode> asPhiNode() {
        return Optional.empty();
    }

    public Optional<ThetaNode> asThetaNode() {
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
        public <R, A> R accept(PegVisitor<R, A> visitor, A arg) {
            return visitor.visit(this, arg);
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

    public final static class LongLit extends PegNode {
        public final long value;
        private LongLit(long value) {
            this.value = value;
            idLookup.put(this.id, this);
            litLookup.put(value, this);
        }

        @Override
        public boolean isIntLit() {
            return true;
        }

        @Override
        public Optional<Long> asLong() {
            return Optional.of(value);
        }

        @Override
        public <R, A> R accept(PegVisitor<R, A> visitor, A arg) {
            return visitor.visit(this, arg);
        }

        @Override
        public boolean isConst() {
            return true;
        }

        @Override
        public String toString() {
            return String.format("%dl", value);
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
        public <R, A> R accept(PegVisitor<R, A> visitor, A arg) {
            return visitor.visit(this, arg);
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
        public <R, A> R accept(PegVisitor<R, A> visitor, A arg) {
            return visitor.visit(this, arg);
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
        public <R, A> R accept(PegVisitor<R, A> visitor, A arg) {
            return visitor.visit(this, arg);
        }

        @Override
        public String toDerefString() {
            final StringJoiner joiner = new StringJoiner(" ", "(", ")");
            joiner.add(op);
            for (Integer child : children) {
                final PegNode p = idLookup.get(child);
                if (p == null) {
                    throw new IllegalStateException("OpNode " + op + " child index " + child + " not present");
                }
                joiner.add(p.toDerefString());
            }

            return joiner.toString();
        }

        @Override
        public boolean getOpNode() {
            return true;
        }

        @Override
        public String toString() {
            StringJoiner joiner = new StringJoiner(" ", "(", ")");
            joiner.add(op);
            for (Integer cid : children) {
                joiner.add(cid.toString());
            }
            return joiner.toString();
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
        public <R, A> R accept(PegVisitor<R, A> visitor, A arg) {
            return visitor.visit(this, arg);
        }

        @Override
        public boolean isPhiNode() {
            return true;
        }

        /**
         * @return the guard node for this {@code PhiNode} if it exists
         * @throws IllegalStateException when the guard id isn't associated with an {@code PegNode}
         */
        public PegNode getGuard() {
            return PegNode.idLookup(guard).orElseThrow(IllegalStateException::new);
        }

        /**
         * @return the then node for this {@code PhiNode} if it exists
         * @throws IllegalStateException when the then id isn't associated with an {@code PegNode}
         */
        public PegNode getThen() {
            return PegNode.idLookup(thn).orElseThrow(IllegalStateException::new);
        }

        /**
         * @return the else node for this {@code PhiNode} if it exists
         * @throws IllegalStateException when the else id isn't associated with an {@code PegNode}
         */
        public PegNode getElse() {
            return PegNode.idLookup(els).orElseThrow(IllegalStateException::new);
        }
    }

    public static class ThetaNode extends OpNode {
        public final Integer init;
        private final BlankNode blank;
        private boolean expand;   // indicates whether to expand during printing

        private ThetaNode(final Integer init, final BlankNode blank) {
            super("theta", init, blank.id);
            this.init = init;
            this.blank = blank;
            this.expand = true;
        }

        private static ThetaNode thetaNode(final Integer init) {
            return new ThetaNode(init, new BlankNode(BlankNode._blankId++));
        }

        @Override
        public Optional<ThetaNode> asThetaNode() {
            return Optional.of(this);
        }

        @Override
        public <R, A> R accept(PegVisitor<R, A> visitor, A arg) {
            return visitor.visit(this, arg);
        }

        @Override
        public boolean isThetaNode() {
            return true;
        }

        @Override
        public String toDerefString() {
            if (!expand) {
                return op;
            }
            expand = false;
            final StringJoiner joiner = new StringJoiner(" ", "(", ")");
            joiner.add(op);
            for (Integer child : children) {
                final PegNode p = idLookup.get(child);
                if (p == null) {
                    throw new IllegalStateException("OpNode " + op + " child index " + child + " not present");
                }
                joiner.add(p.toDerefString());
            }
            expand = true;

            return joiner.toString();
        }

        /**
         * @return the initializer node for this {@code ThetaNode} if it exists
         * @throws IllegalStateException when the initializer id isn't associated with an {@code PegNode}
         */
        public PegNode getInitializer() {
            return PegNode.idLookup(init).orElseThrow(IllegalStateException::new);
        }

        /**
         * A {@code ThetaNode} may be assigned another {@code PegNode} that represents its continuation. This 
         * method checks to see if it is and, if so, returns an {@code Optional.of(PegNode)} wrapping the
         * continuation node. Otherwise, returns empty.
         *
         * @return {@code Optional.of(cont)} if this ThetaNode has been assigned with {@code cont}; if {@code this}
         * isn't identified with any nodes, return {@code Optional.empty()}
         */
        public Optional<PegNode> getContinuation() {
            return Optional.ofNullable(blank.identifiedNode)
                    .flatMap(i -> Optional.ofNullable(idLookup.get(i)));
        }

        public void setContinuation(Integer value) {
            if (blank.identifiedNode != null) {
                throw new IllegalStateException();
            }
            blank.identifiedNode = value;
            equivalences.addEquivalence(blank.id, value);
        }

        private static class BlankNode extends OpNode {
            static int _blankId = 0;
            private final int blankId;
            public Integer identifiedNode;

            private BlankNode(int id) {
                super("blank", intLit(id).id);
                this.blankId = id;
                this.identifiedNode = null;
            }
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
     * Keep track of node equivalences
     */
    private static Equivalences equivalences = new Equivalences();

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

    public static PegNode longLit(long n) {
        if (!litLookup.containsKey(n)) {
            return new LongLit(n);
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

    public static PegNode unit() {
        return opNode("unit");
    }

    public static PhiNode phi(Integer guard, Integer then, Integer els) {
        final String sym = "phi";
        final List<Integer> childs = new ArrayList<>(3);
        childs.add(guard);
        childs.add(then);
        childs.add(els);
        if (!symbolLookup.containsKey(sym) || !symbolLookup.get(sym).containsKey(childs)) {
            return new PhiNode(guard, then, els);
        }
        final PegNode node = symbolLookup.get(sym).get(childs);
        return node.asPhiNode().orElseThrow(() -> new IllegalStateException(
                String.format("Unexpected value cached for sym=\"phi\", children=[%d, %d, %d];" +
                              " expected a PegNode.PhiNode but found %s",
                        guard, then, els, symbolLookup.get(sym).get(childs).toDerefString())));
    }
    
    public static ThetaNode theta(Integer init) {
        return ThetaNode.thetaNode(init);
    }

    public static PegNode var(String name, Integer tpAnnot) {
        return opNode("var", opNode(name).id, tpAnnot);
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

    public static PegNode instanceOf(final Integer val, final String objName) {
        return opNode("instanceof", val, stringLit(objName).id);
    }

    /**
     * Pass node for a theta node
     * @param condition
     * @return
     */
    public static PegNode pass(Integer condition) {
        return opNode("pass", condition);
    }

    /**
     * Eval node for a theta anode
     * @param seq
     * @param pass
     * @return
     */
    public static PegNode eval(Integer seq, Integer pass) {
        return opNode("eval", seq, pass);
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

    public static PegNode nilContext() {
        return opNode("ctx-nil");
    }

    public static PegNode consContext(final String key, final Integer valId, final Integer contextTailId) {
        return opNode("ctx-cons", stringLit(key).id, valId, contextTailId);
    }

    public static PegNode maxExpr(final String startPos, final Integer pegId, final PegContext ctx) {
        return opNode("max-expr", stringLit(startPos).id, pegId, ctx.asPegNode().id, ctx.heap.id);
    }

    /*
     * Arrays literals are formed as linked lists
     */
    public static PegNode nilArray() {
        return opNode("array-nil");
    }

    public static PegNode consArray(final Integer valId, final Integer tailId) {
      return opNode("array-cons", valId, tailId);
    }

    public static PegNode arrayAccess(final Integer nameId, final Integer idxId) {
      return opNode("array-access", nameId, idxId);
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

    // Implement a generic linked list

    /**
     * The Nil from a generic linked list
     * @return an empty linked list
     */
    public static PegNode nil() {
        return opNode("nil");
    }

    /**
     * the Cons operator from a generic linked list
     * @param headId id of the value to be stored
     * @param tailId id of the tail to be stored
     * @return a new linked list with length len(tail) + 1
     */
    public static PegNode cons(final Integer headId, final Integer tailId) {
        return opNode("cons", headId, tailId);
    }


    /**
     * Return a `type-annotation` node. This is used to include type information
     * for values. If no information is given for a value (say, type), `nil` is
     * produced by default.
     *
     * E.g, {@code typeAnnotations("int", null, null)} will produce PegNode
     * {@code (type-annotation (string-lit "int") nil nil)}
     *
     * @param type the literal type of a value
     * @param interfaces the interfaces this value implements
     * @param superClasses the list of superclasses this value's type extends, in order.
     * @return a (type-annotation type interfaces superclasses) node.
     */
    public static PegNode typeAnnotationNode(final String type, final List<String> interfaces, final List<String> superClasses) {
        // First get a stringLit node for type, or `nil` for no type
        final PegNode typeNode = type == null ? nil() : stringLit(type);

        // Next, sort "interfaces" (in reverse order, since we will be creating a linked list out of them). This will
        // give a canonical ordering to the interfaces so that we don't need to do any AC stuff in the egraph. That way
        // ematching will always fire when possible.

        PegNode iList = nil();
        if (interfaces != null) {
            interfaces.sort(Comparator.reverseOrder());
            for (String i : interfaces) {
                iList = cons(stringLit(i).id, iList.id);
            }
        }

        // Finally, let's get the superclasses. These don't need to be sorted because we want the
        // inheritance order to remain intact.

        PegNode scList = nil();
        if (superClasses != null) {
            for (int i = superClasses.size() - 1; i >= 0 ; --i ) {
                scList = cons(stringLit(superClasses.get(i)).id, scList.id);
            }
        }
        return opNode("type-annotation", typeNode.id, iList.id, scList.id);
    }

    /**
     * @param objId id of the object to be cast
     * @param typeId type to cast the object to
     * @return a cast node
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

    /**
     * @param node1 the id of the first PEG to compute a bijection over
     * @param node2 the id of the second PEG to compute a bijection over
     * @return returns true when a bijection exists between the ThetaNode ids
     * such that node1 and node2 are structurally equivalent, otherwise returns false
     * @throws IllegalArgumentException if the node1 or node2 are invalid ids
     */
    public static boolean isStructuralBijection(Integer node1, Integer node2) {
        if (!idLookup.containsKey(node1) || !idLookup.containsKey(node2)) {
            throw new IllegalArgumentException();
        }
        // potential bijection between theta-nodes
        Map<Integer, Integer> bijection1 = new HashMap<>();
        Map<Integer, Integer> bijection2 = new HashMap<>();
        // queue of pegnodes to compare
        Stack<Integer> s1 = new Stack<>();
        Stack<Integer> s2 = new Stack<>();
        s1.add(node1);
        s2.add(node2);
        while (!s1.isEmpty() && !s2.isEmpty()) {
            Integer id1 = s1.pop();
            Integer id2 = s2.pop(); 
            PegNode peg1 = idLookup.get(id1);
            PegNode peg2 = idLookup.get(id2);
            // theta nodes
            if (peg1.isThetaNode() && peg2.isThetaNode()) {
                ThetaNode thetaNode1 = peg1.asThetaNode().get();
                ThetaNode thetaNode2 = peg2.asThetaNode().get();
                // make sure thetas are assigned
                if (!thetaNode1.getContinuation().isPresent() || !thetaNode2.getContinuation().isPresent()) {
                        return false;
                }
                // ids are part of a bijection
                if (bijection1.containsKey(id1) && bijection2.containsKey(id2)) {
                    // make sure its with each other
                    if (!bijection1.get(id1).equals(id2) || !bijection2.get(id2).equals(id1)) {
                        return false;
                    } else {
                        continue;
                    }
                // one id is part of a bijection, but not with the other
                } else if (bijection1.containsKey(id1) || bijection2.containsKey(id2)) {
                    return false;
                // neither id is part of a bijection, so we can form one
                } else {
                    s1.push(thetaNode1.init);
                    s2.push(thetaNode2.init);
                    s1.push(thetaNode1.getContinuation().get().id);
                    s2.push(thetaNode2.getContinuation().get().id);
                    bijection1.put(id1, id2);
                    bijection2.put(id2, id1);
                    continue;
                }
            // opnodes
            } else if (peg1.isOpNode() && peg2.isOpNode()) {
                OpNode opNode1 = peg1.asOpNode().get();
                OpNode opNode2 = peg2.asOpNode().get();
                // make sure operation is the same
                if (!opNode1.op.equals(opNode2.op)) {
                    return false;
                }
                // compare all children
                s1.addAll(opNode1.children);
                s2.addAll(opNode2.children);
            // either different types or both literals
            } else {
                // fail if different types or non-equal literals
                if (!peg1.equals(peg2)) {
                    return false;
                }
            }
        }
        // make sure number of children was consistent
        if (!s1.isEmpty() || !s2.isEmpty()) {
            throw new IllegalStateException();
        }
        return true;
    }
}
