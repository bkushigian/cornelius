package serializer.peg;

import java.util.*;

public abstract class PegNode {

    // TODO: make not static
    public static Map<Integer, PegNode> getIdLookup() {
        return new HashMap<>(idLookup);
    }

    public String toDerefString() {
        return toString();
    }

    static int _id = 0;

    protected final List<Integer> children;
    public final int id;

    PegNode(Integer...children) {
        this.id = _id++;
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


    public static class IntLit extends PegNode {
        public final int value;
        private IntLit(int value) {
            this.value = value;
            idLookup.put(this.id, this);
            litLookup.put(value, this);
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

    public static class BoolLit extends PegNode {
        public final boolean value;
        private BoolLit(boolean value) {
            this.value = value;
            idLookup.put(this.id, this);
            litLookup.put(value, this);
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

    public static class OpNode extends PegNode {
        public final String op;

        private OpNode(String op, Integer...children) {
            super(children);
            this.op = op;
            idLookup.put(id, this);
            symbolLookup.computeIfAbsent(op, x -> new HashMap<>())
                        .put(this.children, this);
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

    public static PegNode unit() {
        return opNode("unit");
    }

    public static PegNode phi(Integer guard, Integer then, Integer els) {
      return opNode("phi", guard, then, els);
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

    // differentiate between heaps
    private static int heapIndex = 1;
    public static PegNode heap() {
      return opNode("heap", intLit(heapIndex++).id);
    }

    /**
     * Get the initial heap, indexed at 0, to represent the heap coming into a method.
     * This should be the same for all methods since we are assuming that all methods are
     * executed with the same heap environment.
     *
     * NOTE: This will have to change for inlining; I'll mark this as a TODO
     * @return the initial heap {@code PegNode}
     */
    public static PegNode initialHeap() {
        return opNode("heap", intLit(0).id);
    }
}
