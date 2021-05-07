public class Loops {

    int program1() {
        /**
         * <cond>
         *  [blank1     (blank-node)
         *   blank2     (blank-node)
         *   state      (theta-node (heap->state heap) blank1)
         *   status     (theta-node (heap->status heap) blank2)
         *   heap       (make-heap state status)
         *   condition  (bool-lit false)]
         * </cond>
         * <expected>
         *  [update     (replace-node blank1 state)
         *   update     (replace-node blank2 status)
         *   pass       (pass-node condition)   
         *   state      (eval-node state pass)
         *   status     (eval-node status pass)
         *   heap       (make-heap state status)
         *   (snapshot {:heap heap})]
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
         *  [blank1         (blank-node)
         *   blank2         (blank-node)
         *   blank3         (blank-node)
         *   blank4         (blank-node)
         *   theta-x        (theta-node (ctx-lookup ctx "x") blank1)
         *   theta-this     (theta-node (ctx-lookup ctx "this") blank2)
         *   theta-state    (theta-node (heap->state heap) blank3)
         *   theta-status   (theta-node (heap->status heap) blank4)
         *   ctx            (ctx-update ctx "x" theta-x)
         *   ctx            (ctx-update ctx "this" theta-this)
         *   heap           (make-heap theta-state theta-status)
         *   condition      (bool-lit false)]
         * </cond>
         * <expected>
         *  [update     (replace-node blank1 (ctx-lookup ctx "x"))
         *   update     (replace-node blank2 (ctx-lookup ctx "this"))
         *   update     (replace-node blank3 (heap->state heap))
         *   update     (replace-node blank4 (heap->status heap))
         *   pass       (pass-node condition)     
         *   ctx        (ctx-update ctx "x" (eval-node theta-x pass))
         *   ctx        (ctx-update ctx "this" (eval-node theta-this pass))
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
         *  [blank1     (blank-node)
         *   blank2     (blank-node)
         *   theta-x    (theta-node (ctx-lookup ctx "x") blank1)
         *   theta-this (theta-node (ctx-lookup ctx "this") blank2)
         *   ctx        (ctx-update ctx "x" theta-x)
         *   ctx        (ctx-update ctx "this" theta-this)
         *   condition  (opnode "<" theta-x (int-lit 3))]
         * </cond> 
         * <expected>     
         *  [update     (replace-node blank1 (ctx-lookup ctx "x"))
         *   update     (replace-node blank2 (ctx-lookup ctx "this"))
         *   pass       (pass-node condition)
         *   ctx        (ctx-update ctx "x" (eval-node theta-x pass))
         *   ctx        (ctx-update ctx "this" (eval-node theta-this pass))
         *   (snapshot {:ctx ctx})]
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
         *  [blank1     (blank-node)
         *   blank2     (blank-node)
         *   theta-x    (theta-node (ctx-lookup ctx "x") blank1)
         *   theta-this (theta-node (ctx-lookup ctx "this") blank2)
         *   cond-x     (opnode "+" theta-x (int-lit 1))
         *   ctx        (ctx-update ctx "x" cond-x)
         *   ctx        (ctx-update ctx "this" theta-this)
         *   condition  (opnode "<" cond-x (int-lit 3))]
         * </cond> 
         * <expected>      
         *  [update     (replace-node blank1 (ctx-lookup ctx "x"))
         *   update     (replace-node blank2 (ctx-lookup ctx "this"))
         *   pass       (pass-node condition)
         *   ctx        (ctx-update ctx "x" (eval-node cond-x pass))
         *   ctx        (ctx-update ctx "this" (eval-node theta-this pass))
         *   (snapshot {:ctx ctx})]
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
         *  [x       (ctx-lookup ctx "x") 
         *   (snapshot {:return x})]
         * </expected>
         */
        return x;
    }
}
