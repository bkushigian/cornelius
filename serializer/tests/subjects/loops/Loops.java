public class Loops {

    int program01() {
        /**
         * <cond>
         *  [state      (theta-node (heap->state heap))
         *   status     (theta-node (heap->status heap))
         *   heap       (make-heap state status)
         *   condition  (bool-lit false)]
         * </cond>
         * <body>
         *  [update     (assign-theta state state)
         *   update     (assign-theta status status)
         *   (snapshot {:ctx ctx :heap heap})]
         * </body> 
         * <expected>
         *  [pass       (pass-node condition)   
         *   state      (eval-node state pass)
         *   status     (eval-node status pass)
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
         *  [ctx     (ctx-update ctx "x" (int-lit 0))]
         * </expected>
         */
        int x = 0;

        /**
         * <cond>
         *  [theta-x        (theta-node (ctx-lookup ctx "x"))
         *   ctx            (ctx-update ctx "x" theta-x)
         *   theta-state    (theta-node (heap->state heap))
         *   theta-status   (theta-node (heap->status heap))
         *   heap           (make-heap theta-state theta-status)
         *   condition      (bool-lit false)]
         * </cond>
         * <body>
         *  [x              (ctx-lookup ctx "x")
         *   state          (heap->state heap)
         *   status         (heap->status heap)
         *   update         (assign-theta theta-x x)
         *   update         (assign-theta theta-state state)
         *   update         (assign-theta theta-status status)
         *   (snapshot {:ctx ctx :heap heap})]
         * </body> 
         * <expected>
         *  [pass       (pass-node condition)     
         *   ctx        (ctx-update ctx "x" (eval-node theta-x pass))
         *   heap       (make-heap (eval-node theta-state pass) (eval-node theta-status pass))     
         *   (snapshot {:ctx ctx :heap heap})]
         * </expected>
         */
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
             *   [x        (ctx-lookup ctx "x")
                  heap     (wr-heap (ctx-lookup ctx "this") "y" x heap)]
             * </expected>
            */
            this.y = x;
        }

        /**
         * <expected>
         *  [x  (ctx-lookup ctx "x") 
         *   (snapshot {:return x})]
         * </expected>
         */
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
         * <cond>
         *  [theta-x        (theta-node (ctx-lookup ctx "x"))
         *   ctx            (ctx-update ctx "x" theta-x)
         *   theta-state    (theta-node (heap->state heap))
         *   theta-status   (theta-node (heap->status heap))
         *   heap           (make-heap theta-state theta-status)
         *   condition      (opnode "<" theta-x (int-lit 3))]
         * </cond> 
         * <body>
         *  [x              (ctx-lookup ctx "x")
         *   state          (heap->state heap)
         *   status         (heap->status heap)
         *   update         (assign-theta theta-x x)
         *   update         (assign-theta theta-state state)
         *   update         (assign-theta theta-status status)
         *   (snapshot {:ctx ctx :heap heap})]
         * </body> 
         * <expected>     
         *  [pass       (pass-node condition)
         *   ctx        (ctx-update ctx "x" (eval-node theta-x pass))
         *   heap       (make-heap (eval-node theta-state pass) (eval-node theta-status pass))
         *   (snapshot {:ctx ctx :heap heap})]
         * </expected>
         */
        while (x < 3) {
            /**
              * <expected>
              *   [x       (ctx-lookup ctx "x")
              *    x-inc   (opnode "+" x (int-lit 1))
              *    ctx     (ctx-update ctx "x" x-inc)]
              * </expected>
            */
            x = x + 1;
        }

        /**
         * <expected>
         *  [x   (ctx-lookup ctx "x") 
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
         * <cond>
         *  [theta-x        (theta-node (ctx-lookup ctx "x"))
         *   theta-state    (theta-node (heap->state heap))
         *   theta-status   (theta-node (heap->status heap))
         *   heap           (make-heap theta-state theta-status)
         *   cond-x         (opnode "+" theta-x (int-lit 1))
         *   ctx            (ctx-update ctx "x" cond-x)
         *   condition      (opnode "<" cond-x (int-lit 3))]
         * </cond>
         * <body>
         *  [x              (ctx-lookup ctx "x")
         *   state          (heap->state heap)
         *   status         (heap->status heap)
         *   update         (assign-theta theta-x x)
         *   update         (assign-theta theta-state state)
         *   update         (assign-theta theta-status status)
         *   (snapshot {:ctx ctx :heap heap})]
         * </body> 
         * <expected>      
         *  [pass       (pass-node condition)
         *   ctx        (ctx-update ctx "x" (eval-node cond-x pass))
         *   heap       (make-heap (eval-node theta-state pass) (eval-node theta-status pass))
         *   (snapshot {:ctx ctx :heap heap})]
         * </expected>
         */
        while ((x = x + 1) < 3) {
            /**
              * <expected>
              *   [x       (ctx-lookup ctx "x")
              *    x-inc   (opnode "+" x (int-lit 1))
              *    ctx     (ctx-update ctx "x" x-inc)]
              * </expected>
            */
            x = x + 1;
        }

        /**
         * <expected>
         *  [x  (ctx-lookup ctx "x") 
         *   (snapshot {:return x})]
         * </expected>
         */
        return x;
    }
}
