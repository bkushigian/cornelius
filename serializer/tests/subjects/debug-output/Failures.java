public class Failures {
    
    public int litFail(int x) {
        /**
         * <expected>
         *   [peg     (opnode "+" (param "x") (int-lit 3))
         *    ctx     (ctx-update ctx "i" peg)
         *    (snapshot {:ctx ctx})]
         * </expected>
         */
        int i = x + 5;

        /**
         * <expected>
         *  [i       (ctx-lookup ctx "i") 
         *   (snapshot {:return i})]
         * </expected>
         */
        return i;
    }

    public int assignFail() {
        /**
         * <cond>
         *  [state      (theta-node (heap->state heap))
         *   status     (theta-node (heap->status heap))
         *   heap       (heap-node state status)
         *   condition  (bool-lit false)]
         * </cond>
         * <body>
         *  [update     (assign-theta state state)
         *   (snapshot {:ctx ctx :heap heap})]
         * </body> 
         * <expected>
         *  [pass       (pass-node condition)   
         *   state      (eval-node state pass)
         *   status     (eval-node status pass)
         *   heap       (heap-node state status)
         *   (snapshot {:ctx ctx :heap heap})]
         * </expected>
         */
        while(false) {}

        /**
         * <expected>
         *  [zero       (int-lit 0) 
         *   (snapshot {:return zero})]
         * </expected>
         */
        return 0;
    }

    public int bijectionFail() {
        /**
         * <expected>
         *   [ctx     (ctx-update ctx "i" (int-lit 0))
         *    ctx     (ctx-update ctx "j" (int-lit 0))]
         * </expected>
         */
        int i = 0;
        int j = 0;

        /**
         * <cond>
         *  [theta-i        (theta-node (ctx-lookup ctx "i"))
         *   theta-j        (theta-node (ctx-lookup ctx "j"))
         *   theta-state    (theta-node (heap->state heap))
         *   theta-status   (theta-node (heap->status heap))
         *   heap           (heap-node theta-state theta-status)
         *   ctx            (ctx-update ctx "i" theta-i)
         *   ctx            (ctx-update ctx "j" theta-j)
         *   condition      (opnode "<" theta-i (int-lit 10))]
         * </cond>
         * <body>
         *  [i              (ctx-lookup ctx "i")
         *   j              (ctx-lookup ctx "j")
         *   state          (heap->state heap)
         *   status         (heap->status heap)
         *   update         (assign-theta theta-i i)
         *   update         (assign-theta theta-j j)
         *   update         (assign-theta theta-state state)
         *   update         (assign-theta theta-status status)
         *   (snapshot {:ctx ctx :heap heap})]
         * </body> 
         * <expected>      
         *  [pass       (pass-node condition)
         *   ctx        (ctx-update ctx "i" (eval-node theta-i pass))
         *   ctx        (ctx-update ctx "j" (eval-node theta-j pass))
         *   heap       (heap-node (eval-node theta-state pass) (eval-node theta-status pass))
         *   (snapshot {:ctx ctx :heap heap})]
         * </expected>
         */
        while (i < 10) {
            /**
             * <expected>
             *   [i       (ctx-lookup ctx "i")
             *    ctx     (ctx-update ctx "temp" i)]
             * </expected>
             */
            int temp = i;

            /**
             * <expected>
             *   [i       (ctx-lookup ctx "i")
             *    ctx     (ctx-update ctx "i" (opnode "+" i i))]
             * </expected>
             */
            i = i + i;

            /**
             * <expected>
             *   [temp    (ctx-lookup ctx "temp")
             *    ctx     (ctx-update ctx "j" (opnode "+" temp temp))]
             * </expected>
             */
            j = j + j;
        }

        /**
         * <expected>
         *  [j  (ctx-lookup ctx "j") 
         *   (snapshot {:return j})]
         * </expected>
         */
        return j;
    }

    
}