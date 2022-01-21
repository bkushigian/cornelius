public class FieldWrite {
    static int s = 0;
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
         * CONDITION FOR IF 1
         * <cond>
         * [guard-1     (ctx-lookup ctx "c")
         *  heap-1-init heap
         *  heap-1      heap
         *  ctx-1-init  ctx
         *  ctx-1       ctx
         *  (snapshot {:peg guard-1})]
         * </cond>
         *
         * <expected>
         * [heap (heap-join guard-1 heap-1 heap-1-init)
         *  ctx  (ctx-join  guard-1 ctx-1  ctx-1-init)
         * (snapshot {:heap heap})
         * ]
         * </expected>
         */
        if (c) {// IF 1
            /**
             * <cond>
             * [ctx-2       ctx-1
             *  ctx-2-init  ctx-1
             *  heap-2      heap-1
             *  heap-2-init heap-1
             *  a (ctx-lookup ctx-2 "a")
             *  y (rd (ctx-lookup ctx "this") "y" heap-1)
             *  guard-2 (opnode "<" a y)
             *  (snapshot {:peg guard-2})
             *  ]
             * </cond>
             * <expected>
             *   [heap-1-then heap-2
             *    heap-1-else heap-4
             *    heap-1      (heap-join guard-2 heap-1-then heap-1-else)
             *    ctx-1-then  ctx-2
             *    ctx-1-else  ctx-4
             *    ctx-1       (ctx-join guard-2 ctx-1-then ctx-1-else)
             *
             *   (snapshot {:heap heap-1})
             *   ]
             * </expected>
             */
            if (a < y) {// IF 2 (Parent 1)
                /**
                 * <expected>
                 * [a    (ctx-lookup ctx-2 "a")
                 *  heap-2 (wr-heap (ctx-lookup ctx-2 "this") "y" a heap-2)
                 *  (snapshot {:heap heap-2 :ctx ctx-2})
                 *  ]
                 * </expected>
                 */
                y = a;

                /**
                 * <cond>
                 * [b (ctx-lookup ctx-2 "b")
                 *  a (ctx-lookup ctx-2 "a")
                 *  guard-3 (opnode "<" b a)
                 *  ctx-3-init  ctx-2
                 *  ctx-3       ctx-2
                 *  heap-3-init heap-2
                 *  heap-3      heap-2
                 *  (snapshot {:peg guard-3})
                 *  ]
                 * </cond>
                 *
                 * <expected>
                 * [heap-2    (heap-join guard-3 heap-3 heap-3-init)
                 *  ctx-2     (ctx-join  guard-3 ctx-3  ctx-3-init)
                 *  (snapshot {:heap heap-2})
                 *  ]
                 * </expected>
                 */
                if (b <= a) { // IF 3
                    /**
                     * <expected>
                     * [b    (ctx-lookup ctx-3 "b")
                     *  heap-3 (wr-heap (ctx-lookup ctx-3 "this") "y" b heap-3)
                     *  (snapshot {:heap heap-3 :ctx ctx-3})
                     *  ]
                     * </expected>
                     */
                    y = b;
                }
            } 
            else
            /**
             * <cond>
             * [ctx-4       ctx-2-init
             *  ctx-4-init  ctx-2-init
             *  heap-4      heap-2-init
             *  heap-4-init heap-2-init
             *  b (ctx-lookup ctx-4 "b")
             *  y (rd (ctx-lookup ctx-4 "this") "y" heap-4)
             *  guard-4 (opnode "<" b y)
             *  (snapshot {:ctx ctx-4 :peg guard-4})
             *  ]
             * </cond>
             * <expected>
             * [heap-4 (heap-join guard-4 heap-4 heap-4-init)
             *  ctx-4  (ctx-join  guard-4 ctx-4  ctx-4-init)
             *  (snapshot {:heap heap-4 :ctx ctx-4})
             *  ]
             * </expected>
             */
            if (b < y) { // IF 4
                /**
                 * <expected>
                 * [b (ctx-lookup ctx-4 "b")
                 *  heap-4 (wr-heap (ctx-lookup ctx-4 "this") "y" b heap-4)
                 *  (snapshot {:heap heap-4})
                 *  ]
                 * </expected>
                 */
                y = b;
            }
        }
        /**
         * <expected>
         * [y (rd (ctx-lookup ctx "this") "y" heap)
         * (snapshot {:return y :heap heap :ctx ctx})]
         * </expected>
         */
        return y;
    }

}
