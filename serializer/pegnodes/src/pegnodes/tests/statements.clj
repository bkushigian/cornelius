(ns pegnodes.tests.statements
  (:require  [clojure.test :as t])
  (:require  [pegnodes.pegs :refer :all])
  (:require  [pegnodes.tests.tests :refer :all]))

(def file-path (str subjects-directory "/statements/Statements.java"))

(def assignments
  '(let [

         ;; Setup
         heap  (initial-heap)
         ctx   (new-ctx-from-params "a")

         ;; x = a + 1
         peg   (opnode "+" (param "a") (int-lit 1))
         ctx   (ctx-update ctx "x" peg)

         ;; y = x + 1
         x     (ctx-lookup ctx "x")
         peg   (opnode "+" x (int-lit 1))
         ctx   (ctx-update ctx "y" peg)

         ;; z = y + x
         y     (ctx-lookup ctx "y")
         x     (ctx-lookup ctx "x")
         peg   (opnode "+" y x)
         ctx   (ctx-update ctx "z" peg)

         ;; return z
         peg   (ctx-lookup ctx "x")
         ]
      (method-root peg heap)))


(def my-tester (init-tester file-path))
(def my-tests (tester->tests my-tester))
(def my-test (first my-tests))
