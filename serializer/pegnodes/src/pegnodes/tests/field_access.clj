(ns pegnodes.tests.field-access
  (:require  [clojure.test :as t])
  (:require  [pegnodes.pegs :refer :all])
  (:require  [pegnodes.tests.tests :refer :all]))

(def file-path (str subjects-directory "/field-access/FieldAccess.java"))

(def simple-field-access
  '(let [heap  (initial-heap)
         peg1  (rd (param "this") "fa" heap)
         heap  (update-exception-status heap (is-null? (param "this")) (exception "java.lang.NullPointerException"))
         peg2  (rd peg1 "fa" heap)
         heap  (update-exception-status heap (is-null? peg1) (exception "java.lang.NullPointerException"))
         peg3  (rd peg2 "y"  heap)
         heap  (update-exception-status heap (is-null? peg2) (exception "java.lang.NullPointerException"))]
      (method-root peg3 heap)))

(def method-invocation
  '(let [heap (initial-heap)
        recv (param  "this")
        args (actuals)
        invk (invoke heap recv "getFieldAccess" args)]
    (method-root (invoke->peg invk) (invoke->heap invk))))

(def method-invocation2
  '(let [heap (initial-heap)
         recv (rd (param "this") "fa" heap)
         args (actuals)
         invk (invoke heap recv "getFieldAccess" args)]
    (method-root (invoke->peg invk) (invoke->heap invk))))

(def programs
  {"methodInvocation()"  method-invocation
   "methodInvocation2()" method-invocation2
   "simpleFieldAccess()" simple-field-access })

(println  (format-test-program method-invocation2))
