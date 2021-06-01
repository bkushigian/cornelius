public class Loops {

    int isEven(int x) {
        return x % 2 == 0;
    }

    int do_nothing_false() {
        /**
         * <cond>
         *  [blank1     (blank-node)
         *   blank2     (blank-node)
         *   state      (theta-node (heap->state heap) blank1)
         *   status     (theta-node (heap->status heap) blank2)
         *   heap       (make-heap state status)
         *   condition  (bool-lit false)
         *   (snapshot {:peg condition :ctx ctx :heap heap})]
         * </cond>
         * <body>
         *  [update     (assign-blank blank1 state)
         *   update     (assign-blank blank2 status)
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

    int false_heap_update() {
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
         *   theta-x        (theta-node (ctx-lookup ctx "x") blank1)
         *   ctx            (ctx-update ctx "x" theta-x)
         *   theta-state    (theta-node (heap->state heap) blank2)
         *   theta-status   (theta-node (heap->status heap) blank3)
         *   heap           (make-heap theta-state theta-status)
         *   condition      (bool-lit false)
         *   (snapshot {:peg condition :ctx ctx :heap heap})]
         * </cond>
         * <body>
         *  [x              (ctx-lookup ctx "x")
         *   state          (heap->state heap)
         *   status         (heap->status heap)
         *   update         (assign-blank blank1 x)
         *   update         (assign-blank blank2 state)
         *   update         (assign-blank blank3 status)
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

    int add_in_body() {
        /**
         * <expected>
         *   [ctx     (ctx-update ctx "x" (int-lit 0))]
         * </expected>
         */
        int x = 0;
        
        /**
         * <cond>
         *  [blank1         (blank-node)
         *   blank2         (blank-node)
         *   blank3         (blank-node)
         *   theta-x        (theta-node (ctx-lookup ctx "x") blank1)
         *   ctx            (ctx-update ctx "x" theta-x)
         *   theta-state    (theta-node (heap->state heap) blank2)
         *   theta-status   (theta-node (heap->status heap) blank3)
         *   heap           (make-heap theta-state theta-status)
         *   condition      (opnode "<" theta-x (int-lit 3))
         *   (snapshot {:peg condition :ctx ctx :heap heap})]
         * </cond> 
         * <body>
         *  [x              (ctx-lookup ctx "x")
         *   state          (heap->state heap)
         *   status         (heap->status heap)
         *   update         (assign-blank blank1 x)
         *   update         (assign-blank blank2 state)
         *   update         (assign-blank blank3 status)
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

    int cond_side_effect() {
        /**
         * <expected>
         *   [ctx     (ctx-update ctx "x" (int-lit 0))]
         * </expected>
         */
        int x = 0;

        /**
         * <cond>
         *  [blank1         (blank-node)
         *   blank2         (blank-node)
         *   blank3         (blank-node)
         *   theta-x        (theta-node (ctx-lookup ctx "x") blank1)
         *   theta-state    (theta-node (heap->state heap) blank2)
         *   theta-status   (theta-node (heap->status heap) blank3)
         *   heap           (make-heap theta-state theta-status)
         *   cond-x         (opnode "+" theta-x (int-lit 1))
         *   ctx            (ctx-update ctx "x" cond-x)
         *   condition      (opnode "<" cond-x (int-lit 3))
         *   (snapshot {:peg condition :ctx ctx :heap heap})]
         * </cond>
         * <body>
         *  [x              (ctx-lookup ctx "x")
         *   state          (heap->state heap)
         *   status         (heap->status heap)
         *   update         (assign-blank blank1 x)
         *   update         (assign-blank blank2 state)
         *   update         (assign-blank blank3 status)
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

    int multiple_vars() {
        /**
         * <expected>
         *   [ctx     (ctx-update ctx "i" (int-lit 0))
         *    ctx     (ctx-update ctx "j" (int-lit 1))]
         * </expected>
         */
        int i = 0;
        int j = 1;

        /**
         * <cond>
         *  [blank1         (blank-node)
         *   blank2         (blank-node)
         *   blank3         (blank-node)
         *   blank4         (blank-node)
         *   theta-i        (theta-node (ctx-lookup ctx "i") blank1)
         *   theta-j        (theta-node (ctx-lookup ctx "j") blank2)
         *   theta-state    (theta-node (heap->state heap) blank3)
         *   theta-status   (theta-node (heap->status heap) blank4)
         *   heap           (make-heap theta-state theta-status)
         *   condition      (opnode "<" cond-i (int-lit 10))
         *   (snapshot {:peg condition :ctx ctx :heap heap})]
         * </cond>
         * <body>
         *  [i              (ctx-lookup ctx "i")
         *   j              (ctx-lookup ctx "j")
         *   state          (heap->state heap)
         *   status         (heap->status heap)
         *   update         (assign-blank blank1 i)
         *   update         (assign-blank blank2 j)
         *   update         (assign-blank blank3 state)
         *   update         (assign-blank blank4 status)
         *   (snapshot {:ctx ctx :heap heap})]
         * </body> 
         * <expected>      
         *  [pass       (pass-node condition)
         *   ctx        (ctx-update ctx "i" (eval-node theta-i pass))
         *   ctx        (ctx-update ctx "j" (eval-node theta-j pass))
         *   heap       (make-heap (eval-node theta-state pass) (eval-node theta-status pass))
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
             *   [j       (ctx-lookup ctx "j")
             *    ctx     (opnode "+" j j)]
             * </expected>
             */
            i = j + j;

            /**
             * <expected>
             *   [temp    (ctx-lookup ctx "temp")
             *    ctx     (opnode "+" temp temp)]
             * </expected>
             */
            j = temp + temp;
        }

        /**
         * <expected>
         *  [j  (ctx-lookup ctx "j") 
         *   (snapshot {:return j})]
         * </expected>
         */
        return j;
    }

    int method_call(int x) {
        int i = 0;
        while(isTrue(i + x)) {
            i++;
        }
    }

    int nested_theta() {
        int i = 0;
        while (i < 5) {
            int j = 0;
            while (i + j < 5) {
	            j++;
            }
            i++;
        }
    }
}
