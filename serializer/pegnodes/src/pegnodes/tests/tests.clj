(ns pegnodes.tests.tests
  (:require  [clojure.test  :as t])
  (:require  [clojure.pprint])
  (:require  [pegnodes.pegs :refer :all])
  (:import   (serializer.peg PegContext PegNode PegSerializer PegCommentScraperVisitor)))

(defn serialize [file]
  (. (PegSerializer. "serialized") serialize file))

(defn scrape-comments
  ([file tag] (PegCommentScraperVisitor/scrape file tag))
  ([file]     (scrape-comments file "target-peg")))

(defn map-over-values
  "map a function over the values of a map."
  [f m]
  (into {} (for [[k v] m] [k (f v)])))

(defn join-tester
  "Transforms tester-map from
  `{:expected {method1 form1} :actual {method1 form2}}`
  to
  `{method1 {:expected form1 :actual form2}}`,
  restricting the domain to keys shared by both maps"
  [tester]
  (let [expected (:expected tester)
        actual   (:actual tester)
        keys     (filter #(contains? expected %1) (keys actual))]
    (into {} (for [k keys] [k {:expected (expected k) :actual (actual k)}]))))

(defn eval-comment-string
  [s]
  (binding [*ns* (find-ns 'pegnodes.pegs)] (eval (read-string s))))

(defn init-tester
  "This initializes a tester from a Java file path. It reads in the file, parses
  each PEG, and parses each method's comments. This produces two maps: one map
  maps method names to the expected result, while the other maps method names to
  the acutal result. These are stored in a lookup table under keys `:actual` and
  `:expected`."
  [file-path]
  ;; TODO: make this more robust. Currently if there is an ill-formed comment
  ;; (e.g., eval fails) then the entire thing fails
  (join-tester {:actual   (into {} (serialize       file-path))
                :expected (map-over-values eval-comment-string
                                           (scrape-comments file-path))}))


(defn index-of-difference
  "Find the index of first difference in two sequences, or nil otherwise."
  [col1 col2]
  (let [result (drop-while #(= (first (second %)) (second (second %)))
                           (map-indexed vector (map vector col1 col2)))]
    (first (first result))))

(defn test-method-output
  "Given a tester set up by `init-tester`, run tests on a method name."
  [tester method-name]
  (let [serialized-str (. (:actual   (tester method-name)) toDerefString)
        expected-str   (. (:expected (tester method-name)) toDerefString)
        idx (index-of-difference serialized-str expected-str)]
    (t/testing method-name
      (t/is (= serialized-str expected-str)
            (str "First difference at index: "
                 idx
                 "\nexpected :"
                 expected-str
                 "\nactual   :"
                 serialized-str
                 "\n"
                 (when (not (nil? idx)) (format "%s^\n" (apply str (repeat (+  idx 10) \-)))))))))

(defn test-java-class
  [file-path]
  (let [tester (init-tester file-path)]
    (t/testing file-path
      (doseq
          [method (keys tester)]
          (test-method-output tester method)))))

(def current-directory  (System/getProperty "user.dir"))
(def subjects-directory "../tests/subjects")

(defn format-test-program
  "Given a lisp expression, format it for embedding in a javadoc comment wrapped
  in an appropriate tag. The tag must not include the open or close angle
  braces '<' or '>'."
  ([name program tag indent]

   (let [pprogram   (with-out-str (clojure.pprint/pprint program))
         lines      (clojure.string/split pprogram #"\n+")
         line-start (str (apply str (repeat indent \space)) \* \space)
         last-line  (str (apply str (repeat indent \space)) \* \/)
         lines      (for [line lines] (str line-start line ))]
     (str (apply str (repeat (- indent 1) \space))
          "/**\n"
          line-start
          name
          \newline
          line-start
          (format "<%s>\n" tag)
          (clojure.string/join \newline lines)
          \newline
          line-start
          (format "</%s>\n" tag)
          last-line)))
  ([name program tag] (format-test-program name program tag 5))
  ([name program] (format-test-program name program "target-peg")))

(defn print-programs-as-java-comments
  [programs]
  (doseq [[k v] programs]
    (printf "%s:\n" k)
    (println (format-test-program k v))
    (println)))
