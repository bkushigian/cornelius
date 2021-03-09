class SideEffects {
    // This adds side effects
    int counter = 0;

    boolean getBoolWithSideEffects() {
        return counter++ % 2 == 0;
    }
    /*********** PRE AND POST INCREMENTS **********/
    int preInc(int a) {
        /**
         *  <expected>
         *  [ctx (ctx-update ctx "a" (opnode "+" (param "a") (int-lit 1)))
         *   lhs (ctx-lookup ctx "a")
         *   rhs (ctx-lookup ctx "a")
         *   (snapshot {:ctx    ctx
         *              :return (opnode "+" lhs rhs)})]
         *  </expected>
         */
        return ++a + a;
    }

    int postInc(int a) {
        /**
         *  <expected>
         *  [lhs (ctx-lookup ctx "a")
         *   ctx (ctx-update ctx "a" (opnode "+" (param "a") (int-lit 1)))
         *   rhs (ctx-lookup ctx "a")
         *   (snapshot {:ctx    ctx
         *              :return (opnode "+" lhs rhs)})]
         *  </expected>
         */
        return a++ + a;
    }

    int preDec(int a) {
        /**
         *  <expected>
         *  [ctx (ctx-update ctx "a" (opnode "-" (param "a") (int-lit 1)))
         *   lhs (ctx-lookup ctx "a")
         *   rhs (ctx-lookup ctx "a")
         *   (snapshot {:ctx    ctx
         *              :return (opnode "+" lhs rhs)})]
         *  </expected>
         */
        return --a + a;
    }

    int postDec(int a) {
        /**
         *  <expected>
         *  [lhs (ctx-lookup ctx "a")
         *   ctx (ctx-update ctx "a" (opnode "-" (param "a") (int-lit 1)))
         *   rhs (ctx-lookup ctx "a")
         *   (snapshot {:ctx    ctx
         *              :return (opnode "+" lhs rhs)})]
         *  </expected>
         */
        return a-- + a;
    }

    /*********** SHORT CIRCUITING OPERATORS **********/
    boolean shortCircuitOr(boolean cond) {
        /**
         * <expected>
         *  [inv  (invoke heap (param "this") "getBoolWithSideEffects" (actuals))
         *   cond (ctx-lookup ctx "cond")
         *
         *   (snapshot {:return (phi cond (bool-lit true) (invoke->peg inv))
         *              :heap   (heap-join cond heap (invoke->heap inv))})
         *   ]
         * </expected>
         */
        return cond || getBoolWithSideEffects();
    }

    /*********** SHORT CIRCUITING OPERATORS **********/
    boolean shortCircuitAnd(boolean cond) {
        /**
         * <expected>
         *  [inv  (invoke heap (param "this") "getBoolWithSideEffects" (actuals))
         *   cond (ctx-lookup ctx "cond")
         *   peg  (phi cond (invoke->peg inv) (bool-lit false))
         *   heap (heap-join cond (invoke->heap inv) heap)
         *   (snapshot {:return peg
         *              :heap   heap})
         *   ]
         * </expected>
         */
        return cond && getBoolWithSideEffects();
    }

    boolean nestedShortCircuit(boolean cond1, boolean cond2) {
        /**
         *  <expected>
         *  [cond1 (ctx-lookup ctx "cond1")
         *   cond2 (ctx-lookup ctx "cond2")
         *   peg (short-circuit-or cond1 cond2)
         *   heap (heap-join cond1 heap heap)
         *   ctx  (ctx-join cond1 ctx ctx)
         *   ths (ctx-lookup ctx "this")
         *   inv (invoke heap ths "getBoolWithSideEffects" (actuals))
         *   inv-peg  (invoke->peg inv)
         *   inv-heap (invoke->heap inv)
         *   peg2 (phi peg (bool-lit true) inv-peg)
         *   heap2 (heap-join peg heap inv-heap)
         *   (snapshot {:return peg2 :heap heap2})
         *   ]
         *  </expected>
         */
        return (cond1 || cond2) || getBoolWithSideEffects();
    }

    SideEffects ref;

    boolean nullCheck() {
        /**
         *  <expected>
         *  [ths       (ctx-lookup ctx "this")
         *   ref       (rd ths "ref" heap)
         *   lhs       (ne ref (null-lit))
         *   heap2     (update-status-npe heap ref)
         *   inv       (invoke heap2 ref "getBoolWithSideEffects" (actuals))
         *   inv-peg   (invoke->peg inv)
         *   inv-heap  (invoke->heap inv)
         *   rhs       (opnode "!" inv-peg)
         *   heap      (heap-join lhs inv-heap heap)
         *   ctx       (ctx-join  lhs ctx ctx)
         *   result    (short-circuit-and lhs rhs)
         *   (snapshot {:heap heap :return result :ctx ctx})
         *   ]
         *
         *  </expected>
         */
        return ref != null && !ref.getBoolWithSideEffects();
    }

    int bytesWritten;
    int charsWritten;
    /**
     *  From org.apache.catalina.connector.OutputBuffer@isNew()::606
     *
     */
    boolean isNew() {
        /**
         *  <expected>
         *  [ths           (ctx-lookup ctx "this")
         *   bytes-written (rd ths "bytesWritten" heap)
         *   lhs           (eq bytes-written (int-lit 0))
         *   chars-written (rd ths "charsWritten" heap)
         *   rhs           (eq chars-written (int-lit 0))
         *   heap          (heap-join lhs heap heap)
         *   ctx       (ctx-join  lhs ctx ctx)
         *   result        (short-circuit-and lhs rhs)
         *   (snapshot {:heap heap :return result :ctx ctx})]
         *  </expected>
         */
        return (bytesWritten == 0) && (charsWritten == 0);
    }

    boolean nestedSideEffects(boolean cond1, boolean cond2) {
        /**
         *  <cond>
         *  [cond1    (ctx-lookup ctx "cond1")
         *   cond2    (ctx-lookup ctx "cond2")
         *   cond     (short-circuit-and cond1 cond2)
         *   heap     (heap-join cond1 heap heap)
         *   ctx      (ctx-join cond1 ctx ctx)
         *   (snapshot {:peg cond :heap heap :ctx ctx})
         *   ]
         *  </cond>
         *
         *  <expected>
         *  [ctx  (ctx-join cond ctx ctx)
         *   heap (heap-join cond heap2 heap)
         *   (snapshot {:heap heap :ctx ctx})]
         *  </expected>
         */
        if (cond1 && cond2) {
            /**
             * <expected>
             * [ref    (rd (ctx-lookup ctx "this") "ref" heap)
             *  heap2  (update-status-npe heap ref)
             *  invk   (invoke heap2 ref "getBoolWithSideEffects" (actuals))
             *  heap2  (invoke->heap invk)
             *  ]
             * </expected>
             */
            ref.getBoolWithSideEffects();
        }

        /**
         * <expected>
         * [result (bool-lit true)
         * (snapshot {:return result :heap heap :ctx ctx})]
         * </expected>
         */
        return true;
    }
}
