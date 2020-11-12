(ns pegnodes.tests.field-access
  (:require  [clojure.test :as t])
  (:require  [pegnodes.pegs :refer :all])
  (:require  [pegnodes.tests.tests :refer :all]))

(def file-path (str subjects-directory "/field-access/FieldAccess.java"))
(test-file file-path)
