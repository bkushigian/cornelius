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
}
