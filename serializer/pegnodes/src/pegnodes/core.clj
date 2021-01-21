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
  (test-files [pegnodes.tests.statements/file-path
               pegnodes.tests.field-access/file-path
               pegnodes.tests.field-write/file-path
               pegnodes.tests.side-effects/file-path]))
