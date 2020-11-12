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

}
