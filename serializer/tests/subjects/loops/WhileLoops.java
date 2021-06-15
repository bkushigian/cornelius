public class WhileLoops {

    int isEven(int x) {
        return x % 2 == 0;
    }

    int doNothingFalse() {
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

    int falseHeapUpdate() {
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

    int simpleBody() {
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

    int condSideEffect() {
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

    int multipleVars() {
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
         *  [theta-i        (theta-node (ctx-lookup ctx "i"))
         *   theta-j        (theta-node (ctx-lookup ctx "j"))
         *   theta-state    (theta-node (heap->state heap))
         *   theta-status   (theta-node (heap->status heap))
         *   heap           (make-heap theta-state theta-status)
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
             *    ctx     (ctx-update ctx "i" (opnode "+" j j))]
             * </expected>
             */
            i = j + j;

            /**
             * <expected>
             *   [temp    (ctx-lookup ctx "temp")
             *    ctx     (ctx-update ctx "j" (opnode "+" temp temp))]
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

    int condMethodCall(int x) {
        /**
         * <expected>
         *   [ctx     (ctx-update ctx "i" (int-lit 0))]
         * </expected>
         */
        int i = 0;

        /**
         * <cond>
         *  [i              (ctx-lookup ctx "i")
         *   theta-i        (theta-node i)
         *   theta-x        (theta-node (param "x"))
         *   theta-state    (theta-node (heap->state heap))
         *   theta-status   (theta-node (heap->status heap))
         *   theta-heap     (make-heap theta-state theta-status)
         *   ctx            (ctx-update ctx "i" theta-i)
         *   ctx            (ctx-update ctx "x" theta-x)
         *   plus           (opnode "+" theta-i theta-x)
         *   ths            (ctx-lookup ctx "this")
         *   inv            (invoke theta-heap ths "isTrue" (actuals plus))
         *   cond-heap      (invoke->heap inv)
         *   condition      (invoke->peg inv)]
         * </cond>
         * <body>
         *  [i              (ctx-lookup ctx "i")
         *   x              (ctx-lookup ctx "x")
         *   cond-state     (heap->state cond-heap)
         *   cond-status    (heap->status cond-heap)
         *   update         (assign-theta theta-i i)
         *   update         (assign-theta theta-x x)
         *   update         (assign-theta theta-state cond-state)
         *   update         (assign-theta theta-status cond-status)
         *   (snapshot {:ctx ctx :heap cond-heap})]
         * </body> 
         * <expected>      
         *  [pass           (pass-node condition)
         *   ctx            (ctx-update ctx "i" (eval-node theta-i pass))
         *   ctx            (ctx-update ctx "x" (eval-node theta-x pass))
         *   heap           (make-heap (eval-node cond-state pass) (eval-node cond-status pass))
         *   (snapshot {:ctx ctx :heap heap})]
         * </expected>
         */
        while(isTrue(i + x)) {
            /**
              * <expected>
              *   [i       (ctx-lookup ctx "i")
              *    i-inc   (opnode "+" i (int-lit 1))
              *    ctx     (ctx-update ctx "i" i-inc)]
              * </expected>
            */
            i++;
        }

        /**
         * <expected>
         *  [i  (ctx-lookup ctx "i") 
         *   (snapshot {:return i})]
         * </expected>
         */
        return i;
    }

    int nestedTheta() {
        /**
         * <expected>
         *   [ctx     (ctx-update ctx "i" (int-lit 0))]
         * </expected>
         */
        int i = 0;

        /**
         * <cond>
         *  [theta-i1       (theta-node (ctx-lookup ctx "i"))
         *   ctx            (ctx-update ctx "i" theta-i1)
         *   theta-state1   (theta-node (heap->state heap))
         *   theta-status1  (theta-node (heap->status heap))
         *   heap           (make-heap theta-state1 theta-status1)
         *   condition1     (opnode "<" theta-i1 (int-lit 5))]
         * </cond> 
         * <body>
         *  [i              (ctx-lookup ctx "i")
         *   state          (heap->state heap)
         *   status         (heap->status heap)
         *   update         (assign-theta theta-i1 i)
         *   update         (assign-theta theta-state1 state)
         *   update         (assign-theta theta-status1 status)
         *   (snapshot {:ctx ctx :heap heap})]
         * </body> 
         * <expected>     
         *  [pass       (pass-node condition1)
         *   ctx        (ctx-update ctx "i" (eval-node theta-i1 pass))
         *   heap       (make-heap (eval-node theta-state1 pass) (eval-node theta-status1 pass))
         *   (snapshot {:ctx ctx :heap heap})]
         * </expected>
         */
        while (i < 5) {

            /**
             * <expected>
             *   [ctx     (ctx-update ctx "j" (int-lit 0))]
             * </expected>
             */
            int j = 0;

            /**
             * <cond>
             *  [theta-i2       (theta-node (ctx-lookup ctx "i"))
             *   theta-j        (theta-node (ctx-lookup ctx "j"))
             *   theta-state2   (theta-node (heap->state heap))
             *   theta-status2  (theta-node (heap->status heap))
             *   heap           (make-heap theta-state2 theta-status2)
             *   ctx            (ctx-update ctx "i" theta-i2)
             *   ctx            (ctx-update ctx "j" theta-j)
             *   plus           (opnode "+" theta-i2 theta-j)
             *   condition2     (opnode "<" plus (int-lit 5))]
             * </cond>
             * <body>
             *  [i              (ctx-lookup ctx "i")
             *   j              (ctx-lookup ctx "j")
             *   state          (heap->state heap)
             *   status         (heap->status heap)
             *   update         (assign-theta theta-i2 i)
             *   update         (assign-theta theta-j j)
             *   update         (assign-theta theta-state2 state)
             *   update         (assign-theta theta-status2 status)]
             * </body> 
             * <expected>      
             *  [pass       (pass-node condition2)
             *   ctx        (ctx-update ctx "i" (eval-node theta-i2 pass))
             *   ctx        (ctx-update ctx "j" (eval-node theta-j pass))
             *   heap       (make-heap (eval-node theta-state2 pass) (eval-node theta-status2 pass))]
             * </expected>
             */
            while (i + j < 5) {
                /**
                 * <expected>
                 *   [j       (ctx-lookup ctx "j")
                 *    j-inc   (opnode "+" j (int-lit 1))
                 *    ctx     (ctx-update ctx "j" j-inc)]
                 * </expected>
                 */
	            j++;
            }

            /**
             * <expected>
             *   [i       (ctx-lookup ctx "i")
             *    i-inc   (opnode "+" i (int-lit 1))
             *    ctx     (ctx-update ctx "i" i-inc)]
             * </expected>
             */
            i++;
        }
    }
}
