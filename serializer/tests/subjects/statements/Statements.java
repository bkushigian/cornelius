/**
 * A simple class to test statement comments
 */
class Statements {
    Statements stmts;
    int value;

    int assignments(int a) {
        /**
         * <expected>
         *   [peg   (opnode "+" (param "a") (int-lit 1))
         *    ctx   (ctx-update ctx "x" peg)
         *    (snapshot {:ctx ctx :heap heap})]
         * </expected>
         */
        int x = a + 1;
        /**
         * <expected>
         *   [x     (ctx-lookup ctx "x")
         *    peg   (opnode "+" x (int-lit 1))
         *    ctx   (ctx-update ctx "y" peg)
         *    (snapshot {:ctx ctx :heap heap})]
         *
         * </expected>
         */
        int y = x + 1;
        /**
         * <expected>
         *   [y     (ctx-lookup ctx "y")
         *    x     (ctx-lookup ctx "x")
         *    peg   (opnode "+" y x)
         *    ctx   (ctx-update ctx "z" peg)
         *    (snapshot {:ctx ctx :heap heap})]
         *   
         * </expected>
         */
        int z = y + x;
        /**
         * <expected>
         *   [peg (ctx-lookup ctx "z")
         *   (snapshot {:return peg})]
         * </expected>
         */
        return z;
    }

    int derefs(int a) {
        /**
         * <expected>
         * [peg (rd (param "this") "stmts" heap)
         *  ctx (ctx-update ctx "s" peg)]
         *  (snapshot {:heap heap :ctx ctx})
         * </expected>
         */
        Statements s = stmts;
        /**
         * <expected>
         * [s    (ctx-lookup ctx "s")
         *  peg  (rd s "value" heap)
         *  heap (update-status-npe heap s)
         *  ctx  (ctx-add-exit-condition ctx (is-null? s))
         *  ctx  (ctx-update ctx "val" peg)]
         *  (snapshot {:heap heap :ctx ctx})
         * </expected>
         */
        int val = s.value;
        /**
         * <expected>
         * [return (ctx-lookup ctx "val")
         *  (snapshot {:return return})]
         * </expected>
         */
        return val;
    }

    int max_no_else(int a, int b) {
        /**
         * <cond>
         * [a (ctx-lookup ctx "a")
         *  b (ctx-lookup ctx "b")
         *  cond (opnode ">" a b)
         *  (snapshot {:peg cond})]
         * </cond>
         * <expected>
         *   [ctx (ctx-join cond ctx-then ctx)
         *    heap (heap-join cond heap heap)
         *    (snapshot {:heap heap :ctx ctx})]
         * </expected>
         *
         */
        if (a > b) {
            /**
             * <expected>
             * [a (ctx-lookup ctx "a")
             *  ctx-then (ctx-update ctx "b" a)
             *  (snapshot {:ctx ctx-then})]
             * </expected>
             */
            b = a;
        }

        /**
         * <expected>
         *  [b (ctx-lookup ctx "b")
         *   (snapshot {:return b})]
         * </expected>
         */
        return b;
    }

    int max_with_else(int a, int b) {
        /**
         * <cond>
         * [a (ctx-lookup ctx "a")
         *  b (ctx-lookup ctx "b")
         *  cond (opnode ">" a b)
         *  (snapshot {:peg cond})]
         * </cond>
         * <expected>
         *   [ctx (ctx-join cond ctx-then ctx-else)
         *    heap (heap-join cond heap heap)
         *    (snapshot {:heap heap :ctx ctx})]
         * </expected>
         *
         */
        if (a > b) {
            /**
             * <expected>
             * [a (ctx-lookup ctx "a")
             *  ctx-then (ctx-update ctx "b" a)
             *  (snapshot {:ctx ctx-then})]
             * </expected>
             */
            b = a;
        } else {
            /**
             * <expected>
             * [b (ctx-lookup ctx "b")
             *  ctx-else (ctx-update ctx "b" b)
             *  (snapshot {:ctx ctx-else})]
             * </expected>
             */
            b = b;
        }

        /**
         * <expected>
         *  [b (ctx-lookup ctx "b")
         *   (snapshot {:return b})]
         * </expected>
         */
        return b;
    }

    int max_with_else_if(int a, int b) {
        /**
         * <cond>
         * [a (ctx-lookup ctx "a")
         *  b (ctx-lookup ctx "b")
         *  cond (opnode ">" a b)
         *  (snapshot {:peg cond})]
         * </cond>
         * <expected>
         *   [ctx (ctx-join cond ctx-then ctx-else)
         *    heap (heap-join cond heap heap-else)
         *    (snapshot {:heap heap :ctx ctx})]
         * </expected>
         *
         */
        if (a > b) {
            /**
             * <expected>
             * [a (ctx-lookup ctx "a")
             *  ctx-then (ctx-update ctx "b" a)
             *  (snapshot {:ctx ctx-then})]
             * </expected>
             */
            b = a;
        }
        else
        /**
         * <cond>
         * [a (ctx-lookup ctx "a")
         *  b (ctx-lookup ctx "b")
         *  cond2 (opnode "<=" a b)
         *  (snapshot {:peg cond2})]
         * </cond>
         *
         * <expected>
         *   [ctx-else (ctx-join cond2 ctx-then-2 ctx)
         *    heap-else (heap-join cond2 heap heap)
         *    (snapshot {:heap heap-else :ctx ctx-else})]
         * </expected>
         */
        if (a <= b)
        {
            /**
             * <expected>
             * [b (ctx-lookup ctx "b")
             *  ctx-then-2 (ctx-update ctx "b" b)
             *  (snapshot {:ctx ctx-then-2})]
             * </expected>
             */
            b = b;
        }
        return b;
    }
}
