(ns pegnodes.core
  (:require  [pegnodes.pegs :refer :all])
  (:require  [pegnodes.tests.tests :refer :all]))

(defn -main
  "Run tests"
  []
  (System/exit
   (test-files
    [(str subjects-directory "/statements/Statements.java")
     (str subjects-directory "/field-access/FieldAccess.java")
     (str subjects-directory "/field-write/FieldWrite.java")
     (str subjects-directory "/side-effects/SideEffects.java")
     (str subjects-directory "/nullity/Nullity.java")
     ])))
