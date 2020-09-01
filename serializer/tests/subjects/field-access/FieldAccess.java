public class FieldAccess {
    public int x = 0;
    public int y = 0;
    FieldAccess fa = this;

    /**
     * This tests more field writing and reading, using overloaded names, and doing multiple rewrites.
     *
     * Expected PEG:
     * <pre>
     * method-root (+ (+ (rd (path (var this) (derefs x)) (heap 0)) (var y)) (rd (path (rd (path (rd (path (var this) (derefs fa)) (heap 0)) (derefs fa)) (heap 0)) (derefs y)) (heap 0))) (wr (path (var this) (derefs y)) (var y) (wr (path (var this) (derefs y)) (var y) (wr (path (var this) (derefs x)) (var ex) (heap 0)))))
     * </pre>
     */
    int test(int ex, int y) {
        // Heap: (heap 0)
        // Context: {ex -> (var ex), y -> (var y)}

        int a = x;
        // Heap: (heap 0)
        // Context:
        //     {
        //       ex -> (var ex),
        //       y  -> (var y),
        //       a  -> (rd (path (var this) (derefs x)) (heap 0))
        //     }

        int b = y;
        // Heap: (heap 0)
        // Context:
        //     {
        //       ex -> (var ex),
        //       y  -> (var y),
        //       a  -> (rd (path (var this) (derefs x)) (heap 0)),
        //       b  -> (var y),
        //     }

        int c = fa.fa.y;
        // Heap: (heap 0)
        // Context:
        //     {
        //       ex -> (var ex),
        //       y  -> (var y),
        //       a  -> (rd (path (var this) (derefs x)) (heap 0)),
        //       b  -> (var y),
        //       // fa: (rd (rd (path (var this) (derefs fa)))
        //       c  -> (rd (path (rd (path (rd (path (var this) (derefs fa)) (heap 0)) (derefs fa)) (heap 0)) (derefs y)) (heap 0))
        //     }

        x = ex;
        // Heap: (wr (path (var this) (derefs x)) (var ex) (heap 0))
        // Context:
        //     {
        //       ex -> (var ex),
        //       y  -> (var y),
        //       a  -> (rd (path (var this) (derefs x)) (heap 0)),
        //       b  -> (var y),
        //       c  -> (rd (path (rd (path (rd (path (var this) (derefs fa)) (heap 0)) (derefs fa)) (heap 0)) (derefs y)) (heap 0))
        //     }

        this.y = y;
        // Heap: (wr (path (var this) (derefs y)) (var y) (wr (path (var this) (derefs x)) (var ex) (heap 0)))
        // Context:
        //     {
        //       ex -> (var ex),
        //       y  -> (var y),
        //       a  -> (rd (path (var this) (derefs x)) (heap 0)),
        //       b  -> (var y),
        //       c  -> (rd (path (rd (path (rd (path (var this) (derefs fa)) (heap 0)) (derefs fa)) (heap 0)) (derefs y)) (heap 0))
        //     }
        this.y = b;
        // Heap: (wr (path (var this) (derefs y)) (var y) (wr (path (var this) (derefs y)) (var y) (wr (path (var this) (derefs x)) (var ex) (heap 0))))
        // Context:
        //     {
        //       ex -> (var ex),
        //       y  -> (var y),
        //       a  -> (rd (path (var this) (derefs x)) (heap 0)),
        //       b  -> (var y),
        //       c  -> (rd (path (rd (path (rd (path (var this) (derefs fa)) (heap 0)) (derefs fa)) (heap 0)) (derefs y)) (heap 0))
        //     }

        int result = a + b + c;
        // Heap: (wr (path (var this) (derefs y)) (var y) (wr (path (var this) (derefs y)) (var y) (wr (path (var this) (derefs x)) (var ex) (heap 0))))
        // Context:
        //     {
        //       ex     -> (var ex),
        //       y      -> (var y),
        //       a      -> (rd (path (var this) (derefs x)) (heap 0)),
        //       b      -> (var y),
        //       c      -> (rd (path (rd (path (rd (path (var this) (derefs fa)) (heap 0)) (derefs fa)) (heap 0)) (derefs y)) (heap 0))
        //       result -> (+ (+ (rd (path (var this) (derefs x)) (heap 0)) (var y)) (rd (path (rd (path (rd (path (var this) (derefs fa)) (heap 0)) (derefs fa)) (heap 0)) (derefs y)) (heap 0)))
        //     }
        return result;
    }


    /**
     * Helper method
     */
    public FieldAccess getFieldAccess() {
        return fa;
    }

    /**
     * This test case checks dereferencing fields
     *
     * Expected PEG:
     * <pre>
     * (method-root (rd (path (rd (path (rd (path (var this) (derefs fa)) (heap 0)) (derefs fa)) (heap 0)) (derefs y)) (heap 0)) (heap 0))
     * </pre>
     */
    int simpleFieldAccess() {
        return this.fa.fa.y;
    }

    /**
     * This test case builts on top of {@code simpleFieldAccess} by adding a
     * method invocation into the field access.
     *
     * Expected PEG:
     * <pre>
     * (method-root (rd (path (rd (path (proj-val (invoke (heap 0) (rd (path (var this) (derefs fa)) (heap 0)) getFieldAccess actuals)) (derefs fa)) (heap 0)) (derefs y)) (heap 0)) (heap 0))
     * </pre>
     */
    int fieldAccessWithMethodInvocation() {
        return fa.getFieldAccess().fa.y;
    }

}
