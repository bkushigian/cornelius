(ns pegnodes.core
  (:require  [pegnodes.pegs :refer :all])
  (:require  [pegnodes.tests.tests :refer :all])
  (:require  [pegnodes.tests.field-access :as field-access]))

(defn -main
  "Run tests"
  []
  (test-java-class field-access/file-path))
