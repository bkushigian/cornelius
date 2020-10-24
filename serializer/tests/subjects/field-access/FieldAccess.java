public class FieldAccess {
    public int x = 0;
    public int y = 0;
    FieldAccess fa = this;

    /**
     * This tests more field writing and reading, using overloaded names, and doing multiple rewrites.
     *
     * Expected PEG:
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
     * <target-peg>
     * (let [heap  (initial-heap)
     *       peg1  (rd (param "this") "fa" heap)
     *       heap  (update-exception-status heap (is-null? (param "this")) (exception "java.lang.NullPointerException"))
     *       peg2  (rd peg1 "fa" heap)
     *       heap  (update-exception-status heap (is-null? peg1) (exception "java.lang.NullPointerException"))
     *       peg3  (rd peg2 "y"  heap)
     *       heap  (update-exception-status heap (is-null? peg2) (exception "java.lang.NullPointerException"))]
     *   (method-root peg3 heap))
     *   
     * </target-peg>
     */
    int simpleFieldAccess() {
        return this.fa.fa.y;
    }

    /**
     * <target-peg>
     * (let [heap (initial-heap)
     *       recv (param  "this")
     *       args (actuals)
     *       invk (invoke heap recv "getFieldAccess" args)]
     *   (method-root (invoke->peg invk) (invoke->heap invk)))
     * </target-peg>
     */
    FieldAccess methodInvocation() {
        return getFieldAccess();
    }

    /**
     * methodInvocation2()
     * <target-peg>
     * (let
     *  [heap
     *   (initial-heap)
     *   recv
     *   (rd (param "this") "fa" heap)
     *   args
     *   (actuals)
     *   invk
     *   (invoke heap recv "getFieldAccess" args)]
     *  (method-root (invoke->peg invk) (invoke->heap invk)))
     * </target-peg>
     */
    FieldAccess methodInvocation2() {
        return fa.getFieldAccess();
    }

    /**
     * methodInvocation3()
     * <target-peg>
     * (let
     *  [heap
     *   (initial-heap)
     *   recv
     *   (rd (param "this") "fa" heap)
     *   args
     *   (actuals)
     *   invk
     *   (invoke heap recv "getFieldAccess" args)
     *   peg1
     *   (invoke->peg invk)
     *   heap
     *   (invoke->heap invk)
     *   peg2
     *   (rd peg1 "fa" heap)
     *   heap
     *   (update-exception-status
     *    heap
     *    (is-null? peg1)
     *    (exception "java.lang.NullPointerException"))]
     *  (method-root peg2 heap))
     * </target-peg>
     */

    FieldAccess methodInvocation3() {
        return fa.getFieldAccess().fa;
    }

    /**
     * methodInvocation4()
     * <target-peg>
     * (let
     *  [heap
     *   (initial-heap)
     *   recv
     *   (rd (param "this") "fa" heap)
     *   args
     *   (actuals)
     *   invk
     *   (invoke heap recv "getFieldAccess" args)
     *   peg1
     *   (invoke->peg invk)
     *   heap
     *   (invoke->heap invk)
     *   peg2
     *   (rd peg1 "fa" heap)
     *   heap
     *   (update-exception-status
     *    heap
     *    (is-null? peg1)
     *    (exception "java.lang.NullPointerException"))
     *   peg
     *   (rd peg2 "y" heap)
     *   heap
     *   (update-exception-status
     *    heap
     *    (is-null? peg2)
     *    (exception "java.lang.NullPointerException"))]
     *  (method-root peg heap))
     * </target-peg>
     */
    int methodInvocation4() {
        return fa.getFieldAccess().fa.y;
    }

    /**
     * This test case tests for guards against NPEs
     * Expected PEG:
     * <-target-peg>
     * (method-root (phi (exit-condition (isnull? (rd (path (var this) (derefs fa)) (heap 0 unit)))) (phi (exit-condition (isnull? (rd (path (var this) (derefs fa)) (heap 0 unit)))) unit (rd (path (rd (path (var this) (derefs fa)) (heap 0 unit)) (derefs x)) (heap 0 unit))) (+ (phi (exit-condition (isnull? (rd (path (var this) (derefs fa)) (heap 0 unit)))) unit (rd (path (rd (path (var this) (derefs fa)) (heap 0 unit)) (derefs x)) (heap 0 unit))) 1)) (heap 0 (phi (isnull? (rd (path (var this) (derefs fa)) (heap 0 unit))) java.lang.NullPointerException unit)))
     * </-target-peg>
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
