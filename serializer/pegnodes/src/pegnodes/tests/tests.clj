(ns pegnodes.tests.tests
  (:require  [clojure.test])
  (:require  [clojure.pprint])
  (:require  [clojure.string])
  (:require  [pegnodes.pegs :refer :all])
  (:import   (serializer.peg PegContext PegNode PegSerializer PegCommentScraperVisitor SimpleJavaToPegTranslator))
  (:import   (com.github.javaparser StaticJavaParser))
  (:import   (com.github.javaparser.ast CompilationUnit))
  (:import   (com.github.javaparser.ast.body MethodDeclaration))
  (:import   (com.github.javaparser.ast.stmt Statement))
  (:import   (com.github.javaparser.printer PrettyPrinter PrettyPrinterConfiguration))
  (:import   (java.io File FileNotFoundException)))

(def JAVAPARSER-DEBUG-PRETTY-PRINT-CONFIGURATION
  (let [config (PrettyPrinterConfiguration.)
        config (. config setPrintJavadoc false)
        config (. config setPrintComments false)
        config (. config setEndOfLineCharacter " ")
        config (. config setIndentSize 0)]
    config))

(defn serialize
  "Return a map `{:pegs pegs :pairs pairs}` where,
  - `peg` is a map from `String`s to `PegNode`s representing method-level pegs,
  - `pairs` is a map from `String`s to `TestPair`s, which represent
    statement-level data that we will use to generate our tests."
  [file]
  (try
    (let [t      (SimpleJavaToPegTranslator. true)
          pegs   (. t translate (StaticJavaParser/parse (File. file)))
          pairs  (. t testPairs)
          params (. pairs paramNameLookup)]
      {:pegs   (into {} pegs)
       :pairs  pairs
       :params (into {} params)})
    (catch FileNotFoundException e (str "Couldn't load file " file))))

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
  "This initializes a tester from a Java file path. A tester is a map from
  method names to a testing 'worklist'. The worklist is a sequence of maps
  corresponding to statements in execution order, with the following keys:

  - `:actual`: the actual `ExpressionResult` produced by the serializer
  - `:expected`: a sequence of operations that should modify state (passed
    through from previous worklist items), to produce expected heap/context data
  - `:node`: the node being visited
  "
  [file-path]
  (let [serialized (serialize file-path)
        {pairs :pairs params :params} serialized
        table (. pairs testPairLookupTable)
        methods  (. table keySet)
        ]
    (into
     {}
     (for [meth methods]
       (do
         (let [worklist
               (into []
                     (for [p (filter #(instance? Statement (. % getNode)) (. table get meth))]
                       (let [node (. p getNode)]
                         {:actual (. p getActual)
                          :expected (try (read-string (. p getExpected))
                                         (catch RuntimeException e []))
                          :node node})))]
           [meth {:worklist  worklist :params (into [] (params meth))}]))))))


(defn index-of-difference
  "Find the index of first difference in two sequences, or nil otherwise."
  [col1 col2]
  (let [result (drop-while #(= (first (second %)) (second (second %)))
                           (map-indexed vector (map vector col1 col2)))
        idx (first (first result))]
    (if (nil? idx)
      (min (count col1) (count col2))
      idx)))

(defn ensure-strings-are-same
  "ensure, using the `testing/is` form, that the two strings are the same. If
  not, report the index of the first difference as well as printing the
  location"

  ([expected-str actual-str] (ensure-strings-are-same expected-str actual-str nil))
  ([expected-str actual-str msg]
   (let [msg (if (nil? msg) "" (str msg ": "))
         idx (index-of-difference expected-str actual-str)]
     (clojure.test/is (= actual-str expected-str)
           (str msg "First difference at index: "
                idx
                "\nexpected :"
                expected-str
                "\nactual   :"
                actual-str
                "\n"
                (when (not (nil? idx))
                  (format "%s%s^\n"
                          (apply str (repeat 10 \space))
                          (apply str (repeat idx \-)))))))))

(defn contexts-are-same
  "Create a test ensuring contexts are the 'same'. By 'same' I mean that each
  context has identical keys, and that `(to-deref-string (ctx-expected key)) =
  (to-deref-string (ctx-actual key))`"
  [ctx-expected ctx-actual]
  (and ctx-expected
       (let [the-keys (into #{} (filter string? (keys ctx-actual)))]
         (list 'clojure.test/testing "CHECKING:CONTEXT"
               (conj (for [k (filter string? the-keys)]
                       `(ensure-strings-are-same (to-deref-string   (~ctx-expected ~k))
                                                 ~(to-deref-string  (ctx-actual   k))
                                                 ~(str "difference at key " k)))
                     'do)))))

(defn pegs-are-same
  [peg-e peg-a]
  (and peg-e
       (list 'clojure.test/testing "CHECKING:PEG"
             `(ensure-strings-are-same
                   (to-deref-string ~peg-e)
                   ~(to-deref-string peg-a)))))

(defn returns-are-same
  [ret-e ret-a]
  (and ret-e
       (list 'clojure.test/testing "CHECKING:RETURN-VALUE"
             `(ensure-strings-are-same
               (to-deref-string ~ret-e)
               ~(to-deref-string ret-a)
               "Return Values Differ"))))

(defn heaps-are-same
  [heap-e heap-a]
  (and heap-e
       (list 'clojure.test/testing "CHECKING HEAP"
             `(ensure-strings-are-same
                   (to-deref-string ~heap-e)
                   ~(to-deref-string heap-a)))))

(defn snapshot->assertion
  "compare a snapshot and an expr-result. If snapshot is nil, return nil.
  Otherwise, write tests comparing it to the `ExprResult`, and return a list
  of tests"
  [snapshot expr-result]
  (and snapshot
       (let [{peg-e :peg ctx-e :ctx heap-e :heap return-e :return} (second snapshot)
             peg-a (.-peg expr-result)
             context-a (.-context expr-result)
             heap-a    (.-heap context-a)
             return-a  (.. context-a getReturnNode)
             ctx-a     (peg-context->ctx context-a)
             heap-comp (heaps-are-same heap-e heap-a)
             ctx-comp  (contexts-are-same ctx-e ctx-a)
             peg-comp  (pegs-are-same peg-e peg-a)
             ret-comp  (returns-are-same return-e return-a)
             xs        (into [] (remove nil? [heap-comp ctx-comp peg-comp ret-comp]))]
         xs)))

(defn- node-debug-info-string
  "Get debug info for a node string"
  [node]
  (let [position (if (.. node getRange isPresent)
                   (let [range (.. node getRange get)
                         begin (.. range begin)
                         begin (format "%d:%d" (.. begin line) (.. begin column))
                         end   (.. range end)
                         end (format "%d:%d" (.. end line) (.. end column))
                         position (format "[%s-%s] " begin end)]
                     position)
                   "")
        meta-model (.. node getMetaModel toString)
        token-range (. node toString JAVAPARSER-DEBUG-PRETTY-PRINT-CONFIGURATION)]
    (format "%s[%s]:\n    %s" position meta-model token-range)))

(defn worklist->test
  "Transform a worklist into a test. Given a worklist
  ```
  [{:actual actual1 :expected expected1},
   {:actual actual2 :expected expected2},
    ...]
  ```
  recursively transform the list into a nested `let` binding structure that
  asserts tests. Each entry in the worklist will have its own `let` binding
  "
  [worklist]
  (when-not (empty? worklist)
    (let [item (first worklist)
          items (rest worklist)
          expected (:expected item)
          actual   (:actual   item)
          node     (:node     item)
          node-str (format "STATEMENT:%s\n" (node-debug-info-string node))
          ;; A stmt's comment contains a set of bindings and an optional
          ;; snapshot. Separate these out, binding them to `snapshots` and
          ;; `bindings`.
          {snapshots true bindings false} (group-by #(and (seq? %) (= (first %) 'snapshot)) expected)
          ;; The (possibly `nil`) result of visiting the rest of the
          ;; worklist `items` recursively
          nested (worklist->test items)
          ;; A possibly nil value that tests if the snapshot matches the
          ;; provided `ExprResult` (i.e., `actual`)
          snapshot-test (snapshot->assertion (first snapshots) actual)
          snapshot-test-wrapped (and snapshot-test `(clojure.test/testing ~node-str ~@snapshot-test))
          ;; Want to create a `let` of the form
          ;; ```
          ;; (let BINDINGS body)
          ;; ```
          ;; where `body` is one of the following:
          ;; 1. If there is a snapshot at this level, then body is
          ;;    ```
          ;;    (clojure.test/testing STMT-STRING SNAPSHOT-TESTS)
          ;;    nested
          ;;    ```
          ;; 2. If there is no snapshot at this level, then body is just `nested`
          ;;
          body (if snapshot-test  (list snapshot-test-wrapped nested) (list nested))
          body (remove nil? body)
          body (if (empty? body) nil body)]
      ;; Just a sanity check, ensure that there are an even number of bindings
      (assert (even? (count bindings)))
      ;; Create a let binding
      (if (nil? bindings) body 
          (concat (list 'let bindings) body)))))

(defn tester->tests
  "Transform a `tester` into a hash map from method names to tests. Each test is
  a `deftest` form that should be defined prior to invoking `runtests`"
  [tester]
  (into {}
        (for [k (keys tester)]

          (let [item     (tester k)
                params   (:params item)
                worklist (:worklist item)
                raw-test (worklist->test worklist)
                the-test  `(clojure.test/deftest
                             ~(symbol (clojure.string/replace (str "test-" k) #"\(\)" "--"))
                             (clojure.test/testing ~(format "METHOD:%s\n" k)
                               (~'let [~'ctx  (new-ctx-from-params ~@params)
                                       ~'heap (initial-heap)]
                                ~raw-test)))
                ]
            [k the-test]))))

(defn file->tests [file-path] (tester->tests (init-tester file-path)))

(defn test-file [file-path]
  (println "TESTING FILE " file-path)
  (doseq [[method the-test] (file->tests file-path)]
    (try
      (binding [*ns* (find-ns 'pegnodes.tests.tests)] (eval the-test))
      (catch RuntimeException e
        (println "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
        (printf "                    %s\n" method)
        (println "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
        (clojure.pprint/pprint the-test)
        (println "[!!!] Error defining above test for" method)
        (println "      Cause:" (:cause (Throwable->map e))))))
  (try
    (let [test-results (binding [*ns* (find-ns 'pegnodes.tests.tests)] (eval `(clojure.test/run-tests)))]
      (println test-results)
      (:fail test-results))
    (catch RuntimeException e
      (println "Error running tests")
      (println "Cause:" (:cause (Throwable->map e)))
      2)))

(defn test-files [file-paths]
  (apply + (for [fp file-paths]
             (test-file fp))))

(def current-directory  (System/getProperty "user.dir"))
(def subjects-directory "../tests/subjects")
