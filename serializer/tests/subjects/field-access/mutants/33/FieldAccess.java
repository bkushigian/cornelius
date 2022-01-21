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

    int simpleFieldAccess() {
        /**
         * simpleFieldAccess()
         * <expected>
         *  [
         *   fa       (rd (param "this") "fa" heap)
         *   heap     (update-status-npe heap (param "this"))
         *   fa-fa    (rd fa "fa" heap)
         *   heap     (update-status-npe heap fa)
         *   fa-fa-y  (rd fa-fa "y" heap)
         *   heap     (update-status-npe heap fa-fa)
         *   (snapshot {:ctx ctx :heap heap :return fa-fa-y})]
         * </expected>
         */
        return this.fa.fa.y;
    }

    FieldAccess methodInvocation() {
        /**
         * <expected>
         *  [recv    (param "this")
         *   args    (actuals)
         *   invk    (invoke heap recv "getFieldAccess" args)
         *   heap    (invoke->heap invk)
         *   peg     (invoke->peg  invk)
         *   (snapshot {:ctx ctx :heap heap :return peg})]
         * </expected>
         */
        return getFieldAccess();
    }

    FieldAccess methodInvocation2() {
        /**
         * <expected>
         *  [recv    (rd (param "this") "fa" heap)
         *   args    (actuals)
         *   invk    (invoke heap recv "getFieldAccess" args)
         *   heap    (invoke->heap invk)
         *   peg     (invoke->peg  invk)
         *   (snapshot {:ctx ctx :heap heap :return peg})]
         * </expected>
         */
        return fa.getFieldAccess();
    }


    FieldAccess methodInvocation3() {
        /**
         * <expected>
         *  [recv    (rd (param "this") "fa" heap)
         *   args    (actuals)
         *   invk    (invoke heap recv "getFieldAccess" args)
         *   heap    (invoke->heap invk)
         *   peg     (invoke->peg  invk)
         *   peg2    (rd peg "fa" heap)
         *   heap    (update-status-npe heap peg)
         *   ctx     (ctx-add-exit-condition ctx (is-null? peg))
         *   (snapshot {:ctx ctx :heap heap :return peg2})]
         * </expected>
         */
        return fa.getFieldAccess().fa;
    }

    int methodInvocation4() {
        /**
         * <expected>
         *  [recv    (rd (param "this") "fa" heap)
         *   args    (actuals)
         *   invk    (invoke heap recv "getFieldAccess" args)
         *   heap    (invoke->heap invk)
         *   peg     (invoke->peg  invk)
         *   peg2    (rd peg "fa" heap)
         *   heap    (update-status-npe heap peg)
         *   ctx     (ctx-add-exit-condition ctx (is-null? peg))
         *   peg     (rd peg2 "y" heap)
         *   heap    (update-status-npe heap peg2)
         *   ctx     (ctx-add-exit-condition ctx (is-null? peg2))
         *   (snapshot {:ctx ctx :heap heap :return peg})]
         * </expected>
         */
        return fa.getFieldAccess().fa.y;
    }

    /**
     * possibleNPEFollowedByContextUpdate()
     * <eexpected>
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
     *   (ctx-add-exit-condition ctx exitc)
     *   peg
     *   peg-tmp
     *   ctx
     *   (ctx-update ctx "x" peg)
     *   x
     *   (ctx-lookup ctx "x")
     *   peg
     *   (opnode "+" x (int-lit 1))
     *   ctx
     *   (ctx-update ctx "x" peg)
     *   peg
     *   (ctx-lookup ctx "x")]
     *  (method-root peg heap))
     * </eexpected>
     */
    int possibleNPEFollowedByContextUpdate() {
        /**
         * <expected>
         *   [fa      (rd (param "this") "fa" heap)
         *    peg     (rd fa "x" heap)
         *    heap    (update-status-npe heap fa)
         *    ctx     (ctx-add-exit-condition ctx (is-null? fa))
         *    ctx     (ctx-update ctx "x" peg)]
         * </expected>
         */
        int x = fa.x;
        /**
         * <expected>
         *   [x     (ctx-lookup ctx "x")
         *    peg   (opnode "+" x (int-lit 1))
         *    ctx   (ctx-update ctx "x" peg)
         *    (snapshot {:heap heap :ctx ctx})]
         * </expected>
         */
        x = x + 1;

        /**
         * <expected>
         *   [x     (ctx-lookup ctx "x")
         *    (snapshot {:return x})]
         *  </expected>
         */
        return x;
    }

    int testFieldAccess1(int ex, int y) {
        /**
         * <expected>
         * [peg   (rd (param "this") "x" heap)
         *  ctx   (ctx-update ctx "a" peg)
         *  (snapshot {:ctx ctx :heap heap})]
         * </expected>
         */
        int a = x;
        /**
         * <expected>
         *  [y   (ctx-lookup ctx "y")
         *   ctx (ctx-update ctx "b" y)
         *   (snapshot {:ctx ctx :heap heap})]
         * </expected>
         */
        int b = y;


        /**
         * <expected>
         *  [fa      (rd (param "this") "fa" heap)
         *   fa-fa   (rd fa "fa" heap)
         *   heap    (update-status-npe heap fa)
         *   ctx     (ctx-add-exit-condition ctx (is-null? fa))
         *   fa-fa-y (rd fa-fa "y" heap)
         *   heap    (update-status-npe heap fa-fa)
         *   ctx     (ctx-add-exit-condition ctx (is-null? fa-fa))
         *   ctx     (ctx-update ctx "c" fa-fa-y)
         *   (snapshot {:ctx ctx :heap heap})]
         * </expected>
         */
        int c = fa.fa.y;

        /**
         * <expected>
         *  [a+b       (opnode "+" (ctx-lookup ctx "a") (ctx-lookup ctx "b"))
         *   a+b+c     (opnode "+" a+b (ctx-lookup ctx "c"))
         *   x         (rd (param "this") "x" heap)
         *   a+b+c+x   (opnode "+" a+b+c x)
         *   ctx       (ctx-update ctx "result" a+b+c+x)
         *   (snapshot {:ctx ctx :heap heap})]
         * </expected>
         */
        int result = (a + b + c) * x;
        /**
         * <expected>
         *   [result (ctx-lookup ctx "result")
         *    (snapshot {:return result})]
         * </expected>
         */
        return result;
    }

    int testFieldAccess2(int ex, int y) {
        /**
         * <expected>
         * [peg   (rd (param "this") "x" heap)
         *  ctx   (ctx-update ctx "a" peg)
         *  (snapshot {:ctx ctx :heap heap})]
         * </expected>
         */
        int a = x;

        /**
         * <expected>
         *  [y   (ctx-lookup ctx "y")
         *   ctx (ctx-update ctx "b" y)
         *   (snapshot {:ctx ctx :heap heap})]
         * </expected>
         */
        int b = y;

        /**
         * <expected>
         *  [fa      (rd (param "this") "fa" heap)
         *   fa-fa   (rd fa "fa" heap)
         *   heap    (update-status-npe heap fa)
         *   ctx     (ctx-add-exit-condition ctx (is-null? fa))
         *   fa-fa-y (rd fa-fa "y" heap)
         *   heap    (update-status-npe heap fa-fa)
         *   ctx     (ctx-add-exit-condition ctx (is-null? fa-fa))
         *   ctx     (ctx-update ctx "c" fa-fa-y)
         *   (snapshot {:ctx ctx :heap heap})]
         * </expected>
         */
        int c = fa.fa.y;

        /**
         * <expected>
         *  [cond  (opnode "<" ex y)
         *   ctx   (ctx-join cond ctx-thn ctx-els)
         *   heap  (heap-join cond heap heap)
         *   (snapshot {:ctx ctx :heap heap})]
         *  </expected>
         */
        if (ex < y) {

            /**
             * <expected>
             *  [y       (ctx-lookup ctx "y")
             *   ctx-thn (ctx-update ctx "a" y)
             *   (snapshot {:ctx ctx-thn})]
             *  </expected>
             */
            a = y;
        } else {
            /**
             * <expected>
             *  [ex      (ctx-lookup ctx "ex")
             *   ctx-els (ctx-update ctx "a" ex)
             *   (snapshot {:ctx ctx-els})]
             *  </expected>
             */
            a = ex;
        }

        /**
         * <expected>
         *  [a+b      (opnode "+" (ctx-lookup ctx "a") (ctx-lookup ctx "b"))
         *   a+b+c    (opnode "+" a+b (ctx-lookup ctx "c"))
         *   this     (ctx-lookup ctx "this")
         *   x        (rd this "x" heap)
         *   a+b+c+x  (opnode "+" a+b+c x)
         *   ctx      (ctx-update ctx "result" a+b+c+x)
         *   (snapshot {:ctx ctx :heap heap})]
         *
         * </expected>
         */
        int result = a + b + c + x;
        /**
         * <expected>
         *  [result   (ctx-lookup ctx "result")
         *   (snapshot {:return result})]
         * </expected>
         */
        return result;
    }
}
