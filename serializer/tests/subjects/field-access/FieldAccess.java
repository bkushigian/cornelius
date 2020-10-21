public class FieldAccess {
    public int x = 0;
    public int y = 0;
    FieldAccess fa = this;

    /**
     * This tests more field writing and reading, using overloaded names, and doing multiple rewrites.
     *
     * Expected PEG:
     * <pre>
     * (method-root (+ (+ (rd (path (var this) (derefs x)) (heap 0)) (var y)) (rd (path (rd (path (rd (path (var this) (derefs fa)) (heap 0)) (derefs fa)) (heap 0)) (derefs y)) (heap 0))) (wr (path (var this) (derefs y)) (var y) (wr (path (var this) (derefs y)) (var y) (wr (path (var this) (derefs x)) (var ex) (heap 0)))))
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
     * <pre>
     * (method-root (invoke->peg (invoke (heap 0 unit) (var this) getFieldAccess actuals)) (heap (invoke->heap-state (invoke (heap 0 unit) (var this) getFieldAccess actuals)) (invoke->exception-status (invoke (heap 0 unit) (var this) getFieldAccess actuals))))
     * </pre>
     */
    FieldAccess methodInvocation() {
        return getFieldAccess();
    }

    /**
     * <pre>
     * (method-root (invoke->peg (invoke (heap 0 unit) (rd (path (var this) (derefs fa)) (heap 0 unit)) getFieldAccess actuals)) (heap (invoke->heap-state (invoke (heap 0 unit) (rd (path (var this) (derefs fa)) (heap 0 unit)) getFieldAccess actuals)) (invoke->exception-status (invoke (heap 0 unit) (rd (path (var this) (derefs fa)) (heap 0 unit)) getFieldAccess actuals))))
     * </pre>
     */
    FieldAccess methodInvocation2() {
        return fa.getFieldAccess();
    }

    /**
     * <pre>
     * (method-root (rd (path (invoke->peg (invoke (heap 0 unit) (rd (path (var this) (derefs fa)) (heap 0 unit)) getFieldAccess actuals)) (derefs fa)) (heap (invoke->heap-state (invoke (heap 0 unit) (rd (path (var this) (derefs fa)) (heap 0 unit)) getFieldAccess actuals)) (invoke->exception-status (invoke (heap 0 unit) (rd (path (var this) (derefs fa)) (heap 0 unit)) getFieldAccess actuals)))) (heap (invoke->heap-state (invoke (heap 0 unit) (rd (path (var this) (derefs fa)) (heap 0 unit)) getFieldAccess actuals)) (phi (isunit? (invoke->exception-status (invoke (heap 0 unit) (rd (path (var this) (derefs fa)) (heap 0 unit)) getFieldAccess actuals))) (phi (isnull? (invoke->peg (invoke (heap 0 unit) (rd (path (var this) (derefs fa)) (heap 0 unit)) getFieldAccess actuals))) java.lang.NullPointerException unit) (invoke->exception-status (invoke (heap 0 unit) (rd (path (var this) (derefs fa)) (heap 0 unit)) getFieldAccess actuals)))))
     * </pre>
     */
    FieldAccess methodInvocation3() {
        return fa.getFieldAccess().fa;
    }

    /**
     * <pre>
     * (method-root (rd (path (rd (path (invoke->peg (invoke (heap 0 unit) (rd (path (var this) (derefs fa)) (heap 0 unit)) getFieldAccess actuals)) (derefs fa)) (heap (invoke->heap-state (invoke (heap 0 unit) (rd (path (var this) (derefs fa)) (heap 0 unit)) getFieldAccess actuals)) (invoke->exception-status (invoke (heap 0 unit) (rd (path (var this) (derefs fa)) (heap 0 unit)) getFieldAccess actuals)))) (derefs y)) (heap (invoke->heap-state (invoke (heap 0 unit) (rd (path (var this) (derefs fa)) (heap 0 unit)) getFieldAccess actuals)) (phi (isunit? (invoke->exception-status (invoke (heap 0 unit) (rd (path (var this) (derefs fa)) (heap 0 unit)) getFieldAccess actuals))) (phi (isnull? (invoke->peg (invoke (heap 0 unit) (rd (path (var this) (derefs fa)) (heap 0 unit)) getFieldAccess actuals))) java.lang.NullPointerException unit) (invoke->exception-status (invoke (heap 0 unit) (rd (path (var this) (derefs fa)) (heap 0 unit)) getFieldAccess actuals))))) (heap (invoke->heap-state (invoke (heap 0 unit) (rd (path (var this) (derefs fa)) (heap 0 unit)) getFieldAccess actuals)) (phi (isunit? (phi (isunit? (invoke->exception-status (invoke (heap 0 unit) (rd (path (var this) (derefs fa)) (heap 0 unit)) getFieldAccess actuals))) (phi (isnull? (invoke->peg (invoke (heap 0 unit) (rd (path (var this) (derefs fa)) (heap 0 unit)) getFieldAccess actuals))) java.lang.NullPointerException unit) (invoke->exception-status (invoke (heap 0 unit) (rd (path (var this) (derefs fa)) (heap 0 unit)) getFieldAccess actuals)))) (phi (isnull? (rd (path (invoke->peg (invoke (heap 0 unit) (rd (path (var this) (derefs fa)) (heap 0 unit)) getFieldAccess actuals)) (derefs fa)) (heap (invoke->heap-state (invoke (heap 0 unit) (rd (path (var this) (derefs fa)) (heap 0 unit)) getFieldAccess actuals)) (invoke->exception-status (invoke (heap 0 unit) (rd (path (var this) (derefs fa)) (heap 0 unit)) getFieldAccess actuals))))) java.lang.NullPointerException unit) (phi (isunit? (invoke->exception-status (invoke (heap 0 unit) (rd (path (var this) (derefs fa)) (heap 0 unit)) getFieldAccess actuals))) (phi (isnull? (invoke->peg (invoke (heap 0 unit) (rd (path (var this) (derefs fa)) (heap 0 unit)) getFieldAccess actuals))) java.lang.NullPointerException unit) (invoke->exception-status (invoke (heap 0 unit) (rd (path (var this) (derefs fa)) (heap 0 unit)) getFieldAccess actuals))))))
     * </pre>
     */
    int methodInvocation4() {
        return fa.getFieldAccess().fa.y;
    }

    /**
     * This test case tests for guards against NPEs
     * Expected PEG:
     * <pre>
     * (method-root (phi (exit-condition (isnull? (rd (path (var this) (derefs fa)) (heap 0 unit)))) (phi (exit-condition (isnull? (rd (path (var this) (derefs fa)) (heap 0 unit)))) unit (rd (path (rd (path (var this) (derefs fa)) (heap 0 unit)) (derefs x)) (heap 0 unit))) (+ (phi (exit-condition (isnull? (rd (path (var this) (derefs fa)) (heap 0 unit)))) unit (rd (path (rd (path (var this) (derefs fa)) (heap 0 unit)) (derefs x)) (heap 0 unit))) 1)) (heap 0 (phi (isnull? (rd (path (var this) (derefs fa)) (heap 0 unit))) java.lang.NullPointerException unit)))
     * </pre>
     */
    int possibleNPEFollowedByContextUpdate() {
        // Heap:      (heap 0 unit)
        // Context:   {}
        // ExitConds: []
        int x = fa.x;
        // this.fa:
        //     Heap: (heap 0 unit)
        //     ExitConds: []
        //     PEG1: (rd (path (var this) (derefs fa)) (heap 0 unit))
        //     Context: {}
        //
        // this.fa.x:
        //     Heap: (heap 0 (phi (exit-condition (isnull? (rd (path (var this) (derefs fa)) (heap 0 unit)))) java.lang.NullPointerException unit))
        //     ExitConds: [(isnull? (rd (path (var this) (derefs fa)) (heap 0 unit)))]
        //     PEG2: (rd (path (rd (path (var this) (derefs fa)) (heap 0 unit)) (derefs x)) (heap 0 unit))
        //     Context: {}
        //    
        //     NOTES: The exception status is now a PHI node that evaluates to
        //        `java.lang.NullPointerException` whenever this.x is `null`; It
        //        is `unit` otherwise.
        // assignment:
        //     Heap: (heap 0 (phi (isnull? (rd (path (var this) (derefs fa)) (heap 0 unit))) java.lang.NullPointerException unit))
        //     ExitConds: [(isnull? (rd (path (var this) (derefs fa)) (heap 0 unit)))]
        //     PEG2: (rd (path (rd (path (var this) (derefs fa)) (heap 0 unit)) (derefs x)) (heap 0 unit))
        //     Context: {x -> (phi (exit-condition (isnull? (rd (path (var this) (derefs fa)) (heap 0 unit)))) unit (rd (path (rd (path (var this) (derefs fa)) (heap 0 unit)) (derefs x)) (heap 0 unit)))}
        //
        //     NOTES: The assignment updates the context, but only when all exit
        //         conditions are false. The heap is the same, as are the exit
        //         conditions. The context now maps x to a PHI node that branches
        //         over the exit condition.
        //
        //         The THEN branch contains unit, since this means that a NPE
        //         occurred before `x` could be assigned a value (note, x
        //         already was assigned to a value, this value would replace
        //         `unit`).
        //
        //         The ELSE branch contains a `rd` node corresponding to
        //         derefrencing (this.fa).x. This represents a successful read.

        x = x + 1;
        // x = x + 1:
        //
        //     Heap: (heap 0 (phi (isnull? (rd (path (var this) (derefs fa)) (heap 0 unit))) java.lang.NullPointerException unit))
        //     Context: {x -> (phi (exit-condition (isnull? (rd (path (var this) (derefs fa)) (heap 0 unit)))) (phi (exit-condition (isnull? (rd (path (var this) (derefs fa)) (heap 0 unit)))) unit (rd (path (rd (path (var this) (derefs fa)) (heap 0 unit)) (derefs x)) (heap 0 unit))) (+ (phi (exit-condition (isnull? (rd (path (var this) (derefs fa)) (heap 0 unit)))) unit (rd (path (rd (path (var this) (derefs fa)) (heap 0 unit)) (derefs x)) (heap 0 unit))) 1))}
        //
        //     NOTES: We are updating x again, and this assignment needs to be
        //         wrapped in a guard against a possible NPE. This is actually a
        //         little unsatisfying since we've already checked for a NPE,
        //         and this is a possible future improvement
        //
        //         The heap and exit conditions are not updated at all
        return x;
        // The resulting method-root is:
        // (method-root (phi (exit-condition (isnull? (rd (path (var this) (derefs fa)) (heap 0 unit)))) (phi (exit-condition (isnull? (rd (path (var this) (derefs fa)) (heap 0 unit)))) unit (rd (path (rd (path (var this) (derefs fa)) (heap 0 unit)) (derefs x)) (heap 0 unit))) (+ (phi (exit-condition (isnull? (rd (path (var this) (derefs fa)) (heap 0 unit)))) unit (rd (path (rd (path (var this) (derefs fa)) (heap 0 unit)) (derefs x)) (heap 0 unit))) 1)) (heap 0 (phi (isnull? (rd (path (var this) (derefs fa)) (heap 0 unit))) java.lang.NullPointerException unit)))
    }
}
