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
         *  [c1 (ctx-lookup ctx "cond1")
         *   c2 (ctx-lookup ctx "cond2")
         *   peg (phi cond1 (bool-lit true) cond2)
         *   inv (invoke invoke heap (param "this") "getBoolWithSideEffects" (actuals))
         *   inv-peg (invoke->peg inv)
         *   inv-heap (invoke->heap inv)
         *   peg2 (phi peg true inv-peg)
         *   heap (phi peg heap inv-heap)
         *   (snapshot {:return peg2 :heap heap})
         *   ]
         *  </expected>
         */
        return (cond1 || cond2) || getBoolWithSideEffects();
    }

    SideEffects ref;

    boolean nullCheck() {
        return ref != null && !ref.getBoolWithSideEffects();
    }

    int bytesWritten;
    int charsWritten;
    /**
     *  From org.apache.catalina.connector.OutputBuffer@isNew()::606
     *
     */
    boolean isNew() {
        return (bytesWritten == 0) && (charsWritten == 0);
    }
}
