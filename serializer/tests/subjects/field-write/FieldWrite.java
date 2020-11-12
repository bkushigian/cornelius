public class FieldWrite {
    int x = 0;


    int test_field_write(int a) {
        /**
         * <expected>
         * [ctx (ctx-update ctx "result" (int-lit 0))]
         * </expected>
         */
        int result = 0;
        /**
         * <expected>
         * [a1 (ctx-lookup ctx "a")
         *  a2 (ctx-lookup ctx "a")
         *  peg (opnode "-" a1 a2)
         *  (snapshot {:ctx ctx})
         *  ]
         * </expected>
         */
        x = a - a;
        result = x;
        return result;
    }

    /**
     * This test case tests field writes and reads with the `max` program
     *
     * Expected PEG:
     * <pre>
     * (method-root (rd (path (var this) (derefs x)) (wr (path (var this) (derefs x)) (phi (> (var a) (var b)) (var a) (var b)) (heap 0))) (wr (path (var this) (derefs x)) (phi (> (var a) (var b)) (var a) (var b)) (heap 0)))
     * </pre>
     */
    int test_heapy_max(int a, int b) {
        x = a > b ? a : b;
        int result = x;
        return result;
    }

}
