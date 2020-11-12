(ns pegnodes.core
  (:require  [pegnodes.pegs :refer :all])
  (:require  [pegnodes.tests.tests :refer :all])
  (:require  [pegnodes.tests.field-access])
  (:require  [pegnodes.tests.statements])
  )

(defn -main
  "Run tests"
  []
  ;; (test-file pegnodes.tests.field-access/file-path)
  (test-file pegnodes.tests.statements/file-path))
