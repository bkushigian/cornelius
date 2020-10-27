(ns pegnodes.tests.field-access
  (:require  [clojure.test :as t])
  (:require  [pegnodes.pegs :refer :all])
  (:require  [pegnodes.tests.tests :refer :all]))

(def file-path (str subjects-directory "/field-access/FieldAccess.java"))

(def simple-field-access
  '(let [heap  (initial-heap)
         peg1  (rd (param "this") "fa" heap)
         heap  (update-status-npe heap (param "this"))
         peg2  (rd peg1 "fa" heap)
         heap  (update-status-npe heap peg1)
         peg3  (rd peg2 "y"  heap)
         heap  (update-status-npe heap peg2)]
      (method-root peg3 heap)))

(def method-invocation
  '(let [heap (initial-heap)
        recv (param  "this")
        args (actuals)
        invk (invoke heap recv "getFieldAccess" args)]
    (method-root (invoke->peg invk) (invoke->heap invk))))

(def method-invocation-2
  '(let [heap (initial-heap)
         recv (rd (param "this") "fa" heap)
         args (actuals)
         invk (invoke heap recv "getFieldAccess" args)]
    (method-root (invoke->peg invk) (invoke->heap invk))))

(def method-invocation-3
  '(let [heap (initial-heap)
         recv (rd (param "this") "fa" heap)
         args (actuals)
         invk (invoke heap recv "getFieldAccess" args)
         peg1 (invoke->peg  invk)
         heap (invoke->heap invk)
         peg2 (rd peg1 "fa" heap)
         heap (update-status-npe heap peg1)]
    (method-root peg2 heap)))

(def method-invocation-4
  '(let [heap (initial-heap)
         recv (rd (param "this") "fa" heap)
         args (actuals)
         invk (invoke heap recv "getFieldAccess" args)
         peg1 (invoke->peg  invk)
         heap (invoke->heap invk)
         peg2 (rd peg1 "fa" heap)
         heap (update-status-npe heap peg1)
         peg  (rd peg2 "y" heap)
         heap (update-status-npe heap peg2)]

    (method-root peg heap)))

(def possible-npe-followed-by-cntext-update
  '(let [heap  (initial-heap)
         ctx   (new-ctx-from-params)
         ;; fa
         peg      (rd (param "this") "fa" heap)
         ;; fa.x
         peg-tmp  (rd peg "x" heap)
         exitc    (is-null? peg)
         heap     (update-status-npe heap peg)
         ctx      (add-exit-condition-to-ctx ctx exitc)
         peg      peg-tmp               ; fa.x
         ;; x = fa.x
         ctx      (update-key-in-ctx ctx "x" peg)
         ;; x + 1
         x        (lookup-in-ctx ctx "x")
         peg      (opnode "+" x (int-lit 1))
         ;; x = x + 1
         ctx      (update-key-in-ctx ctx "x" peg)
         ;; return x
         peg      (lookup-in-ctx ctx "x")]
     (method-root peg heap)))

(def test-field-access-1
  '(let [heap (initial-heap)
         ctx  (new-ctx-from-params "ex" "y")
         ;; int a = x
         peg     (rd (param "this") "x" heap)
         ctx     (update-key-in-ctx ctx "a" peg)
         ;;  int b = y
         y       (lookup-in-ctx ctx "y")
         ctx     (update-key-in-ctx ctx "b" y)
         ;; fa
         fa      (rd (param "this") "fa" heap)
         ;; fa.fa
         fa-fa   (rd fa "fa" heap)
         heap    (update-status-npe heap fa)
         ctx     (add-exit-condition-to-ctx ctx (is-null? fa))
         ;; fa.fa.y
         fa-fa-y (rd fa-fa "y" heap)
         heap    (update-status-npe heap fa-fa)
         ctx     (add-exit-condition-to-ctx ctx (is-null? fa-fa))
         ;;  int c = fa.fa.y
         ctx     (update-key-in-ctx ctx "c" fa-fa-y)
         ;; a + b + c + x
         ;; This is left-associative so parses as ((a + b) + c) + x
         a+b     (opnode "+" (lookup-in-ctx ctx "a") (lookup-in-ctx ctx "b"))
         a+b+c   (opnode "+" a+b (lookup-in-ctx ctx "c"))
         x       (rd (param "this") "x" heap)
         a+b+c+x (opnode "+" a+b+c x)
         ctx     (update-key-in-ctx ctx "result" a+b+c+x)
         result  (lookup-in-ctx ctx "result")
         ]
    (method-root result heap)))

(def test-field-access-2
  '(let [heap (initial-heap)
         ctx  (new-ctx-from-params "ex" "y")
         ;; int a = x
         peg     (rd (param "this") "x" heap)
         ctx     (update-key-in-ctx ctx "a" peg)
         ;;  int b = y
         y       (lookup-in-ctx ctx "y")
         ctx     (update-key-in-ctx ctx "b" y)
         ;; fa
         fa      (rd (param "this") "fa" heap)
         ;; fa.fa
         fa-fa   (rd fa "fa" heap)
         heap    (update-status-npe heap fa)
         ctx     (add-exit-condition-to-ctx ctx (is-null? fa))
         ;; fa.fa.y
         fa-fa-y (rd fa-fa "y" heap)
         heap    (update-status-npe heap fa-fa)
         ctx     (add-exit-condition-to-ctx ctx (is-null? fa-fa))
         ;;  int c = fa.fa.y
         ctx     (update-key-in-ctx ctx "c" fa-fa-y)
         ;; if (ex < y)
         ex      (lookup-in-ctx ctx "ex")
         y       (lookup-in-ctx ctx "y")
         cond    (opnode "<" ex y)
         ctx-thn (update-key-in-ctx ctx "a" y)
         ctx-els (update-key-in-ctx ctx "a" ex)
         ctx     (ctx-join cond ctx-thn ctx-els)
         ;; a + b + c + x
         ;; This is left-associative so parses as ((a + b) + c) + x
         a+b     (opnode "+" (lookup-in-ctx ctx "a") (lookup-in-ctx ctx "b"))
         a+b+c   (opnode "+" a+b (lookup-in-ctx ctx "c"))
         x       (rd (param "this") "x" heap)
         a+b+c+x (opnode "+" a+b+c x)
         ctx     (update-key-in-ctx ctx "result" a+b+c+x)
         result  (lookup-in-ctx ctx "result")
         ]
    (method-root result heap)))

(def expected-pegs
  "A map from method names, as presented by JavaParser, to programs producing
  their expected PEGs. These can be printied using
  `(pegnodes.tests.tests/print-programs-as-java-comments expected-pegs)`."
  {"methodInvocation()"  method-invocation
   "methodInvocation2()" method-invocation-2
   "methodInvocation3()" method-invocation-3
   "methodInvocation4()" method-invocation-4
   "simpleFieldAccess()" simple-field-access
   "possibleNPEFollowedByContextUpdate()" possible-npe-followed-by-cntext-update
   "testFieldAccess1(int,int)" test-field-access-1
   })
