(ns pegnodes.core
  (:require  [pegnodes.pegs :refer :all])
  (:require  [pegnodes.tests.tests :refer :all])
  (:require  [pegnodes.tests.statements])
  (:require  [pegnodes.tests.field-access])
  (:require  [pegnodes.tests.field-write])
  (:require  [pegnodes.tests.side-effects]))

(defn -main
  "Run tests"
  []
  (test-files
   [(str subjects-directory "/statements/Statements.java")
    (str subjects-directory "/field-access/FieldAccess.java")
    (str subjects-directory "/field-write/FieldWrite.java")
    (str subjects-directory "/side-effects/SideEffects.java")
    (str subjects-directory "/nullity/Nullity.java")
    ]))
