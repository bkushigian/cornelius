class SideEffects {
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

    int preDec(int a) {
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
    boolean shortCircuit(boolean cond) {
        /**
         * <expected>
         *  [inv (invoke heap (param "this") "getBoolWithSideEffects" (actuals))
         *   hp2 (invoke->heap inv)
         *   peg (invoke->peg  inv)
         *   (snapshot {:peg inv :return (phi (param "cond") (bool-lit false) peg)})
         *   ]
         * </expected>
         */
        return cond || getBoolWithSideEffects();
    }

}
