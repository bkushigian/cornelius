public class Loops {

    int simpleFor() {
        /**
         * <expected>
         *  [ctx     (ctx-update ctx "x" (int-lit 1))]
         * </expected>
         */
        int x = 1;

        /**
         * <init>
         *  [ctx     (ctx-update ctx "i" (int-lit 0))
         *   (snapshot {:ctx ctx :heap heap})]
         * </init> 
         * <cond>
         *  [theta-i        (theta-node (ctx-lookup ctx "i"))
         *   theta-x        (theta-node (ctx-lookup ctx "x"))
         *   ctx            (ctx-update ctx "i" theta-i)
         *   ctx            (ctx-update ctx "x" theta-x)
         *   theta-state    (theta-node (heap->state heap))
         *   theta-status   (theta-node (heap->status heap))
         *   heap           (heap-node theta-state theta-status)
         *   condition      (opnode "<" theta-i (int-lit 5))]
         * </cond> 
         * <body>
         *  [i              (ctx-lookup ctx "i")
         *   x              (ctx-lookup ctx "x")
         *   i-inc          (opnode "+" i (int-lit 1))
         *   ctx            (ctx-update ctx "i" i-inc)
         *   state          (heap->state heap)
         *   status         (heap->status heap)
         *   update         (assign-theta theta-i i-inc)
         *   update         (assign-theta theta-x x)
         *   update         (assign-theta theta-state state)
         *   update         (assign-theta theta-status status)
         *   (snapshot {:ctx ctx :heap heap})]
         * </body>
         * <expected>     
         *  [pass       (pass-node condition)
         *   ctx        (ctx-update ctx "i" (eval-node theta-i pass))
         *   ctx        (ctx-update ctx "x" (eval-node theta-x pass))
         *   heap       (heap-node (eval-node theta-state pass) (eval-node theta-status pass))
         *   (snapshot {:ctx ctx :heap heap})]
         * </expected>
         */
        for (int i = 0; i < 5; i++) {
            /**
             * <expected>
             *   [x       (ctx-lookup ctx "x")
             *    i       (ctx-lookup ctx "i")
             *    ctx     (ctx-update ctx "x" (opnode "+" x i))]
             * </expected>
             */
            x = x + i;
        }

        /**
         * <expected>
         *  [x  (ctx-lookup ctx "x") 
         *   (snapshot {:return x})]
         * </expected>
         */
        return x;
    }

    int multipleStatements(int a, int b) {
        /**
         * <init>
         *  [ab      (opnode "+" (param "a") (param "b"))
         *   c       ab
         *   ctx     (ctx-update ctx "c" ab)
         *   ctx     (ctx-update ctx "d" (opnode "+" ab c))
         *   ctx     (ctx-update ctx "e" (int-lit 1))
         *   (snapshot {:ctx ctx :heap heap})]
         * </init> 
         * <cond>
         *  [theta-a        (theta-node (ctx-lookup ctx "a"))
         *   theta-b        (theta-node (ctx-lookup ctx "b"))
         *   theta-c        (theta-node (ctx-lookup ctx "c"))
         *   theta-d        (theta-node (ctx-lookup ctx "d"))
         *   theta-e        (theta-node (ctx-lookup ctx "e"))
         *   ctx            (ctx-update ctx "a" theta-a)
         *   ctx            (ctx-update ctx "b" theta-b)
         *   ctx            (ctx-update ctx "c" theta-c)
         *   ctx            (ctx-update ctx "d" theta-d)
         *   ctx            (ctx-update ctx "e" theta-e)
         *   theta-state    (theta-node (heap->state heap))
         *   theta-status   (theta-node (heap->status heap))
         *   heap           (heap-node theta-state theta-status)
         *   ca-b           (opnode "-" (opnode "*" theta-c theta-a) theta-b)
         *   condition      (opnode "<" theta-d ca-b)]
         * </cond> 
         * <body>
         *  [a              (ctx-lookup ctx "a")
         *   b              (ctx-lookup ctx "b")
         *   c              (ctx-lookup ctx "c")
         *   d              (ctx-lookup ctx "d")
         *   e              (ctx-lookup ctx "e")
         *   a-dec          (opnode "-" a (int-lit 1))
         *   b-dec          (opnode "-" b (int-lit 1))
         *   c-inc          (opnode "+" c (int-lit 1))
         *   ctx            (ctx-update ctx "a" a-dec)
         *   ctx            (ctx-update ctx "b" b-dec)
         *   ctx            (ctx-update ctx "c" c-inc)
         *   state          (heap->state heap)
         *   status         (heap->status heap)
         *   update         (assign-theta theta-a a-dec)
         *   update         (assign-theta theta-b b-dec)
         *   update         (assign-theta theta-c c-inc)
         *   update         (assign-theta theta-d d)
         *   update         (assign-theta theta-e e)
         *   update         (assign-theta theta-state state)
         *   update         (assign-theta theta-status status)
         *   (snapshot {:ctx ctx :heap heap})]
         * </body>
         * <expected>     
         *  [pass       (pass-node condition)
         *   ctx        (ctx-update ctx "a" (eval-node theta-a pass))
         *   ctx        (ctx-update ctx "b" (eval-node theta-b pass))
         *   ctx        (ctx-update ctx "c" (eval-node theta-c pass))
         *   ctx        (ctx-update ctx "d" (eval-node theta-d pass))
         *   ctx        (ctx-update ctx "e" (eval-node theta-e pass))
         *   heap       (heap-node (eval-node theta-state pass) (eval-node theta-status pass))
         *   (snapshot {:ctx ctx :heap heap})]
         * </expected>
         */
        for (
            // INITIALIZER
            int c = a + b,
                d = a + b + c,
                e = 1;
                // CONDITION
                d < c * a - b;
                // UPDATE
                a--, b--, c++)
        {
            /**
             * <expected>
             *   [b       (ctx-lookup ctx "b")
             *    ctx     (ctx-update ctx "a" (opnode "-" b (int-lit 1)))]
             * </expected>
             */
            a = b - 1;

            /**
             * <expected>
             *   [a       (ctx-lookup ctx "a")
             *    ctx     (ctx-update ctx "b" (opnode "+" a (int-lit 14)))]
             * </expected>
             */
            b = a + 14;
        }

        /**
         * <expected>
         *  [ab  (opnode "*" (ctx-lookup ctx "a") (ctx-lookup ctx "b")) 
         *   (snapshot {:return ab})]
         * </expected>
         */
        return a * b;
    }
    


    int y;

    boolean isEven(int x) {
        return (x % 2) == 0;
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
         *   heap           (heap-node theta-state theta-status)
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
         *   heap       (heap-node (eval-node theta-state pass) (eval-node theta-status pass))
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
        int x = 0;

        while ((x = x + 1) < 3) {
            x = x + 1;
        }

        return x;
    }

    int multipleVars() {
        int i = 0;
        int j = 1;

        while (i < 10) {
            int temp = i;

            i = j + j;

            j = temp + temp;
        }

        return j;
    }

    int condMethodCall(int x) {
        int i = 0;

        while(isEven(i + x)) {
            i++;
        }

        return i;
    }

    int nestedTheta() {
        int i = 0;

        while (i < 5) {

            int j = 0;

            while (i + j < 5) {
	            j++;
            }

            i++;
        }
        return i;
    }
}
