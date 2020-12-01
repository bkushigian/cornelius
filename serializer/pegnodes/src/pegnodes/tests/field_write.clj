(ns pegnodes.tests.field-write
  (:require  [clojure.test :as t])
  (:require  [pegnodes.pegs :refer :all])
  (:require  [pegnodes.tests.tests :refer :all]))

(def file-path (str subjects-directory "/field-write/FieldWrite.java"))
