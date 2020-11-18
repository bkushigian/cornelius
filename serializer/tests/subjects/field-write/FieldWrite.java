public class FieldWrite {
    int x = 0;

    int test_field_write(int a) {
        /**
         * <expected>
         *   [ctx (ctx-update ctx "result" (int-lit 0))
         *    (snapshot {:ctx ctx})]
         * </expected>
         */
        int result = 0;

        /**
         * <expected>
         *   [a1  (ctx-lookup ctx "a")
         *    a2  (ctx-lookup ctx "a")
         *    peg (opnode "-" a1 a2)
         *    heap (wr-heap (param "this") "x" peg heap)
         *    (snapshot {:ctx ctx :heap heap})]
         * </expected>
         */
        x = a - a;

        /**
         * <expected>
         *   [x   (rd (param "this") "x" heap)
         *    ctx (ctx-update ctx "result" x)
         *    (snapshot {:heap heap :ctx ctx})]
         * </expected>
         */
        result = x;

        /**
         * <expected>
         *   [peg   (ctx-lookup ctx "result")
         *    (snapshot {:return peg :ctx ctx :heap heap})]
         * </expected>
         */
        return result;
    }

    int test_heapy_max(int a, int b) {
        /**
         * <expected>
         * [a (ctx-lookup ctx "a")
         *  b (ctx-lookup ctx "b")
         *  guard (opnode ">" a b)
         *  thn (ctx-lookup ctx "a")
         *  els (ctx-lookup ctx "b")
         *  peg (phi guard thn els)
         *  ctx  (ctx-join guard ctx ctx)
         *  heap (heap-join guard heap heap)
         *  heap (wr-heap (ctx-lookup ctx "this") "x" peg heap)
         *  (snapshot {:ctx ctx :heap heap})
         *  ]
         *  </expected>
         */
        x = a > b ? a : b;

        /**
         * <expected>
         * [peg (rd (ctx-lookup ctx "this") "x" heap)
         *  ctx (ctx-update ctx "result" peg)
         *  (snapshot {:ctx ctx})]
         * </expected>
         */
        int result = x;
        /**
         * <expected>
         * [result (ctx-lookup ctx "result")
         *  (snapshot {:return result :ctx ctx :heap heap})]
         * </expected>
         */
        return result;
    }

    int y;
    int nestedHeapyWrites(int a, int b, boolean c) {
        /**
         * <cond>
         * [guard-1 (ctx-lookup ctx "c")
         *  heap-1 heap
         *  ctx-1 ctx
         *  (snapshot {:peg guard-1})]
         * </cond>
         *
         * <expected>
         * [heap (heap-join guard-1 heap-then heap-else)
         * (snapshot {:heap heap})
         * ]
         * </expected>
         */
        if (c) {
            /**
             * <cond>
             * [a (ctx-lookup ctx "a")
             *  y (rd (ctx-lookup ctx "this") "y" heap-1)
             *  guard-2 (opnode "<" a y)
             *  heap-2 heap
             *  ctx-2 ctx
             *  (snapshot {:peg guard-2})]
             * </cond>
             * <expected>
             *   [heap-then (heap-join guard-2 heap-then heap-else)
             *   (snapshot {:heap heap-then})]
             * </expected>
             */
            if (a < y) {
                /**
                 * <expected>
                 * [a    (ctx-lookup ctx "a")
                 *  heap (wr-heap (ctx-lookup ctx "this") "y" a heap)
                 *  (snapshot {:heap heap :ctx ctx})]
                 * </expected>
                 */
                y = a;

                /**
                 * <cond>
                 * [b (ctx-lookup ctx "b")
                 *  a (ctx-lookup ctx "a")
                 *  guard-3 (opnode "<" b a)
                 *  ctx-3 ctx
                 *  heap-3 heap
                 *  (snapshot {:peg guard-3})]
                 * </cond>
                 *
                 * <expected>
                 * [heap-then (heap-join guard-3 heap heap-3)
                 *  ctx-then  (ctx-join  guard-3 ctx  ctx-3)
                 *  (snapshot {:heap heap-then :ctx ctx-then})]
                 * </expected>
                 */
                if (b < a) {
                    /**
                     * <expected>
                     * [b    (ctx-lookup ctx "b")
                     *  heap (wr-heap (ctx-lookup ctx "this") "y" b heap)
                     *  (snapshot {:heap heap})]
                     * </expected>
                     */
                    y = b;
                }
            } 
            else
                /**
                 * <cond>
                 * [ctx ctx-2
                 *  heap heap-2
                 *  ctx-4 ctx
                 *  heap-4 heap
                 *  b (ctx-lookup ctx "b")
                 *  y (rd (ctx-lookup ctx "this") "y" heap)
                 *  guard-4 (opnode "<" b y)
                 *  (snapshot {:peg guard-4})
                 *  ]
                 * </cond>
                 * <expected>
                 * [heap-else (heap-join guard-4 heap-5 heap-4)
                 *  ctx  (ctx-join  guard-4 ctx-4 ctx-4)
                 *  (snapshot {:heap heap-else :ctx ctx})]
                 * </expected>
                 */
                if (b < y) {
                /**
                 * <expected>
                 * [b (ctx-lookup ctx "b")
                 *  heap-5 (wr-heap (ctx-lookup ctx "this") "y" b heap-4)
                 *  (snapshot {:heap heap-5})]
                 * </expected>
                 */
                y = b;
            }
        } 
        return y;
    }

}
