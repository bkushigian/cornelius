;; Copyright (C) 2020 Ben Kushigian
;;
;; Author: Ben Kushigian <http://github/benku>
;; Maintainer: Ben Kushigian <bkushigian@gmail.com>
;; Created: October 21, 2020
;; Modified: October 21, 2020
;; Version: 0.1.0-SNAPSHOT
;; Keywords:
;; Homepage: https://github.com/benku/pegs

(ns pegnodes.pegs
  (:import (serializer.peg PegContext PegNode))
  (:gen-class))

(defn  is-valid-id?
  "Check to ensure we are not creating a potential circular dependency. A valid
  id is an id already stored in the lookup table. Since ids are stored
  sequentially, this is equivalent to checking if the size of the idLookupTable
  is larger than the id."
  [id]
  (> (. (PegNode/getIdLookup) size) id))

(defn object->id
  "Turns an object into an Integer id. Works on PegNodes, Longs, and Integers;
  throws an IllegalArgumentException for all other inputs. If an invalid id is
  provided, throws an IllegalArgumentException."
  [obj]

  (let [id (cond
             (int? obj)               (int obj)
             (instance? PegNode obj)  (.-id obj)
             :else  (throw (IllegalArgumentException. (str "Bad type for opnode obj: " obj))))]
    (if (is-valid-id? id)
      id
      (throw (IllegalStateException. "Invalid ID detected")))))

(defn opnode
  "Create an arbitrary opnode PEG."
  [sym & children]
  (let [children2 (map object->id children)]
    (PegNode/opNode sym (into-array Integer children2))))

(defn int-lit
  "Create an integer literal PEG"
  [n]
  (PegNode/intLit (int n)))

(defn bool-lit
  "Create a boolean literal PEG"
  [b]
  (PegNode/boolLit (boolean b)))

(defn unit
  "Create a unit PEG"
  []
  (PegNode/unit))

(defn phi
  "Create a phi node (if/then/else expression)."
  [cond then else]
  (PegNode/phi (object->id cond) (object->id then) (object->id else)))

(defn param
  "Create a parameter node"
  [var-name]
  (PegNode/var var-name))

(defn derefs
  "Create a derefs node"
  [field-name]
  (PegNode/derefs field-name))

(defn path
  "Create a path node"
  [base field]
  (cond
    (instance? String field) (PegNode/path (object->id base) (object->id (derefs field)))
    :else                    (PegNode/path (object->id base) (object->id field))))

(defn rd
  ([path heap]       (PegNode/rd (object->id path) (object->id heap)))
  ([base field heap] (rd (path base field) heap)))

(defn wr
  ([path value heap]     (PegNode/wr (object->id path) (object->id value) (object->id heap)))
  ([base field value heap] (wr (path base field) value heap)))

(defn invoke [heap receiver method actuals]
  (PegNode/invoke (object->id heap) (object->id receiver) (str method) (object->id actuals)))

(defn actuals [& xs]
  (PegNode/actuals (into-array Integer (map object->id xs))))

(defn invoke->peg [invocation]
  (PegNode/invokeToPeg (object->id invocation)))

(defn invoke->heap-state [invocation]
  (PegNode/invocationToHeapState (object->id invocation)))

(defn invoke->exception-status [invocation]
  (PegNode/invocationToExceptionStatus (object->id invocation)))

(defn invoke-threw? [invocation]
  (PegNode/invocationThrew (object->id invocation)))

(defn invoke->heap [invocation]
  (PegNode/projectHeap (object->id invocation)))

(defn heap [state status]
  (PegNode/heap (object->id state) (object->id status)))

(defn initial-heap [] (PegNode/initialHeap))

(defn wr-heap
  ([path val heap] (PegNode/wrHeap (object->id path) (object->id val) (object->id heap)))
  ([base field value heap] (wr-heap (path base field) value heap)))

(defn null-lit []
  (PegNode/nullLit))

(defn is-null? [x]
  (PegNode/isnull (object->id x)))

(defn is-unit? [x]
  (PegNode/isunit (object->id x)))

(defn exception [name]
  (PegNode/exception name))

(defn exit-conditions [conditions]
  (PegNode/exitConditions conditions))

(defn id-lookup [id]
  (cond (int? id) (. (PegNode/idLookup (int id)) orElse nil)
        :else nil))

(defn print-id-table []
  (let  [table (PegNode/getIdLookup)
         keys  (. table keySet)]
    (doseq [id keys]
      (printf "%4d: %s\n" id (. table get id)))))

(defn update-exception-status [old-status-or-heap condition exception]
  "Update a heap or an exception status with a new condition and exception.
OLD-STATUS: the old status to be updated
CONDITION: the condition when an exception status is thrown
EXCEPTION: the exception to be thrown"
  (let [node (cond (int? old-status-or-heap) (id-lookup (int old-status-or-heap))
                   (instance? PegNode old-status-or-heap) old-status-or-heap
                   :else (throw  (IllegalArgumentException. "old-status-or-heap must be a PegNode or an Id (i.e., integer type)")))]
    (cond (. node isHeap)    (heap (. node state) (update-exception-status (. node status) condition exception))
          (. node isOpNode) (phi (is-unit? node)
                                (phi condition
                                     exception
                                     (unit))
                                node)
          :else (throw (IllegalArgumentException. "Expected an OpNode of some sort")))))

(defn foo [] 1)
