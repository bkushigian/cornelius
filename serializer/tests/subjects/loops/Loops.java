public class Loops {

    int program1() {
        /**
         * <expected>
         *  [blank1     (blank)
         *   blank2     (blank)
         *   state      (theta (heap->state heap) blank1)
         *   status     (theta (heap->status heap) blank2)
         *   update     (repl blank1 state)
         *   update     (repl blank2 status)
         *   pass       (opnode "pass" (bool-lit false))   
         *   state      (opnode "eval" state pass)
         *   status     (opnode "eval" status pass)
         *   heap       (make-heap state status)
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

    int program02() {
        /**
         * <expected>
         *   [ctx     (ctx-update ctx "x" (int-lit 0))]
         * </expected>
         */
        int x = 0;

        
        while (false) {
            /**
             * <expected>
             *   [x     (ctx-lookup ctx "x")
             *    peg   (opnode "+" x (int-lit 1))
             *    ctx   (ctx-update ctx "x" peg)]
             * </expected>
             */
            x = x + 1;
            
            /**
             * <expected>
             *   [peg     (wr (param "this") "y" x heap)
             * </expected>
            */
            this.y = x;
            // STATE: (wr "this.y" (theta (heap unit 0)  ))

        }
        /// DONE VISITING BODY. What's the heap? What's the context?

        // Context: {x: (eval (theta 0 (+ THIS_THETA 1)) (pass (theta false THIS_THETA)))}
        // Heap: (heap (eval (theta unit THIS_THETA) (pass (theta false THIS_THETA)))
        //             (eval (theta 0 THIS_THETA)    (pass (theta false THIS_THETA))))
        // PASS
        return x;
    }

    int program03() {
        /**
         * <expected>
         *   [ctx     (ctx-update ctx "x" (int-lit 0))]
         * </expected>
         */
        int x = 0;
        
        /**
         * <expected>
         *  [blank1     (blank)
         *   x          (theta (ctx-lookup ctx "x") blank1)
         *   x-inc      (opnode "+" x (int-lit 1))
         *   update     (repl blank1 x-inc)
         *   pass       (opnode "pass" (opnode "<" x (int-lit 3)))   
         *   ctx        (ctx-update ctx "x" (opnode "eval" x pass))
         *   (snapshot {:ctx ctx})]
         * </expected>
         */
        while (x < 3) {
            x = x + 1;
        }

        /**
         * <expected>
         *  [x       (ctx-lookup ctx "x") 
         *   (snapshot {:return x})]
         * </expected>
         */
        return x;
    }

    int program04() {
        /**
         * <expected>
         *   [ctx     (ctx-update ctx "x" (int-lit 0))]
         * </expected>
         */
        int x = 0;

        /**
         * <expected>
         *  [blank1         (blank)
         *   x              (theta (ctx-lookup ctx "x") blank1)
         *   x-after-cond   (opnode "+" x (int-lit 1))
         *   x-after-body   (opnode "+" x-after-cond (int-lit 1))
         *   update         (repl blank1 x-after-body)
         *   pass           (opnode "pass" (opnode "<" x-after-cond (int-lit 3)))   
         *   ctx            (ctx-update ctx "x" (opnode "eval" x pass))
         *   (snapshot {:ctx ctx})]
         * </expected>
         */
        while ((x = x + 1) < 3) {
            x = x + 1;
        }

        /**
         * <expected>
         *  [x       (ctx-lookup ctx "x") 
         *   (snapshot {:return x})]
         * </expected>
         */
        return x;
    }
}
