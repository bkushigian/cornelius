public class FieldAccess {
    public int x = 0;
    public int y = 0;
    FieldAccess fa = this;

    /**
     * Helper method
     */
    public FieldAccess getFieldAccess() {
        return fa;
    }

    /**
     * simpleFieldAccess()
     * <target-peg>
     * (let
     *  [heap
     *   (initial-heap)
     *   peg1
     *   (rd (param "this") "fa" heap)
     *   heap
     *   (update-status-npe heap (param "this"))
     *   peg2
     *   (rd peg1 "fa" heap)
     *   heap
     *   (update-status-npe heap peg1)
     *   peg3
     *   (rd peg2 "y" heap)
     *   heap
     *   (update-status-npe heap peg2)]
     *  (method-root peg3 heap))
     * </target-peg>
     */
    int simpleFieldAccess() {
        return this.fa.fa.y;
    }

    /**
     * methodInvocation()
     * <target-peg>
     * (let
     *  [heap
     *   (initial-heap)
     *   recv
     *   (param "this")
     *   args
     *   (actuals)
     *   invk
     *   (invoke heap recv "getFieldAccess" args)]
     *  (method-root (invoke->peg invk) (invoke->heap invk)))
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
     *   (update-status-npe heap peg1)]
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
     *   (update-status-npe heap peg1)
     *   peg
     *   (rd peg2 "y" heap)
     *   heap
     *   (update-status-npe heap peg2)]
     *  (method-root peg heap))
     * </target-peg>
     */
    int methodInvocation4() {
        return fa.getFieldAccess().fa.y;
    }

    /**
     * possibleNPEFollowedByContextUpdate()
     * <target-peg>
     * (let
     *  [heap
     *   (initial-heap)
     *   ctx
     *   (new-ctx-from-params)
     *   peg
     *   (rd (param "this") "fa" heap)
     *   peg-tmp
     *   (rd peg "x" heap)
     *   exitc
     *   (is-null? peg)
     *   heap
     *   (update-status-npe heap peg)
     *   ctx
     *   (add-exit-condition-to-ctx ctx exitc)
     *   peg
     *   peg-tmp
     *   ctx
     *   (update-key-in-ctx ctx "x" peg)
     *   x
     *   (lookup-in-ctx ctx "x")
     *   peg
     *   (opnode "+" x (int-lit 1))
     *   ctx
     *   (update-key-in-ctx ctx "x" peg)
     *   peg
     *   (lookup-in-ctx ctx "x")]
     *  (method-root peg heap))
     * </target-peg>
     */
    int possibleNPEFollowedByContextUpdate() {
        int x = fa.x;
        x = x + 1;
        return x;
    }

    /**
     * testFieldAccess1(int,int)
     * <target-peg>
     * (let
     *  [heap
     *   (initial-heap)
     *   ctx
     *   (new-ctx-from-params "ex" "y")
     *   peg
     *   (rd (param "this") "x" heap)
     *   ctx
     *   (update-key-in-ctx ctx "a" peg)
     *   y
     *   (lookup-in-ctx ctx "y")
     *   ctx
     *   (update-key-in-ctx ctx "b" y)
     *   fa
     *   (rd (param "this") "fa" heap)
     *   fa-fa
     *   (rd fa "fa" heap)
     *   heap
     *   (update-status-npe heap fa)
     *   ctx
     *   (add-exit-condition-to-ctx ctx (is-null? fa))
     *   fa-fa-y
     *   (rd fa-fa "y" heap)
     *   heap
     *   (update-status-npe heap fa-fa)
     *   ctx
     *   (add-exit-condition-to-ctx ctx (is-null? fa-fa))
     *   ctx
     *   (update-key-in-ctx ctx "c" fa-fa-y)
     *   a+b
     *   (opnode "+" (lookup-in-ctx ctx "a") (lookup-in-ctx ctx "b"))
     *   a+b+c
     *   (opnode "+" a+b (lookup-in-ctx ctx "c"))
     *   x
     *   (rd (param "this") "x" heap)
     *   a+b+c+x
     *   (opnode "+" a+b+c x)
     *   ctx
     *   (update-key-in-ctx ctx "result" a+b+c+x)
     *   result
     *   (lookup-in-ctx ctx "result")]
     *  (method-root result heap))
     * </target-peg>
     */
    int testFieldAccess1(int ex, int y) {
        int a = x;
        int b = y;
        int c = fa.fa.y;

        int result = a + b + c + x;
        return result;
    }

    /**
     * testFieldAccess2(int,int)
     * <target-peg>
     * (let
     *  [heap
     *   (initial-heap)
     *   ctx
     *   (new-ctx-from-params "ex" "y")
     *   peg
     *   (rd (param "this") "x" heap)
     *   ctx
     *   (update-key-in-ctx ctx "a" peg)
     *   y
     *   (lookup-in-ctx ctx "y")
     *   ctx
     *   (update-key-in-ctx ctx "b" y)
     *   fa
     *   (rd (param "this") "fa" heap)
     *   fa-fa
     *   (rd fa "fa" heap)
     *   heap
     *   (update-status-npe heap fa)
     *   ctx
     *   (add-exit-condition-to-ctx ctx (is-null? fa))
     *   fa-fa-y
     *   (rd fa-fa "y" heap)
     *   heap
     *   (update-status-npe heap fa-fa)
     *   ctx
     *   (add-exit-condition-to-ctx ctx (is-null? fa-fa))
     *   ctx
     *   (update-key-in-ctx ctx "c" fa-fa-y)
     *   ex
     *   (lookup-in-ctx ctx "ex")
     *   y
     *   (lookup-in-ctx ctx "y")
     *   cond
     *   (opnode "<" ex y)
     *   ctx-thn
     *   (update-key-in-ctx ctx "a" y)
     *   ctx-els
     *   (update-key-in-ctx ctx "a" ex)
     *   ctx
     *   (ctx-join cond ctx-thn ctx-els)
     *   a+b
     *   (opnode "+" (lookup-in-ctx ctx "a") (lookup-in-ctx ctx "b"))
     *   a+b+c
     *   (opnode "+" a+b (lookup-in-ctx ctx "c"))
     *   x
     *   (rd (param "this") "x" heap)
     *   a+b+c+x
     *   (opnode "+" a+b+c x)
     *   ctx
     *   (update-key-in-ctx ctx "result" a+b+c+x)
     *   result
     *   (lookup-in-ctx ctx "result")]
     *  (method-root result heap))
     * </target-peg>
     */
    int testFieldAccess2(int ex, int y) {
        int a = x;
        int b = y;
        int c = fa.fa.y;

        if (ex < y) {
            a = y;
        } else {
            a = ex;
        }

        int result = a + b + c + x;
        return result;
    }
}
