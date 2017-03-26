(ns double-booked.core
  (:require [clj-time.coerce :as c]
            [clojure.core.reducers :as reducers]
            [clojure.math.combinatorics :as combo]))

(defrecord Event [name start end])

(defn make-event [name start end]
  (->Event name (/ (c/to-long start) 3600000) (/ (c/to-long end) 3600000)))

(defrecord EventConflict [name1 name2])

(defn make-conflict [event-names]
  (->EventConflict (first event-names) (second event-names)))

(defrecord EventComponent [timestamp type name])

(defn event-to-components [e]
  [(->EventComponent (get e :start) 'start (get e :name))
   (->EventComponent (get e :end) 'end (get e :name))])

(def a (Event. "My Birthday" "05-31-95T00:00:00" "05-31-95T23:59:59"))

; Define

(def t1 (make-event "My Birthweek" "1995-05-31" "1995-06-07"))
(def t2 (make-event "My Birthday" "1995-05-31" "1995-06-01"))
(def t3 (make-event "First of June" "1995-06-01" "1995-06-02"))
(def t4 (make-event "Winter Break" "1995-12-22" "1996-01-05"))
(def t5 (make-event "Christmas" "1995-12-25" "1995-12-26"))



(defn overlap? [event-pair]
  (or  (and (<= (get (first event-pair) :start) (get (second event-pair) :end))
            (>= (get (first event-pair) :end) (get (second event-pair) :start)))
       (and (<= (get (second event-pair) :start) (get (first event-pair) :end))
            (>= (get (second event-pair) :end) (get (first event-pair) :start)))))


; get-conflicts-naive is the naive implementation of this problem
; essentially, compare each event to all other events, checking for overlaps
; this approach is extremely lightweight, and even though it has a complexity
; of O(n^2), it is faster than the approach outlined above
(defn get-conflicts-naive [list-of-events]
  (let [event-pairs (combo/combinations list-of-events 2)]
    (filter overlap? event-pairs)))


(defn walk-intervals [components active conflicts]
  (cond
    (empty? components) conflicts
    (= 'start (get (first components) :type))
     (let [next-active (conj active (get (first components) :name))]
       (walk-intervals (rest components)
                       next-active
                       (conj conflicts (map make-conflict (combo/combinations next-active 2)))))
    (= 'end (get (first components) :type))
     (let [next-active (disj active  (get (first components) :name))]
       (walk-intervals (rest components)
                       next-active
                       conflicts))))

; get-conflicts-intervals splits the events into their start and end
; components, and sorts them. once sorted, it returns all events that have
; starts within other events. looking for nested ends is redundant.
(defn get-conflicts-intervals [list-of-events]
  (let [components (map event-to-components list-of-events)
        sorted-components (sort-by (fn [x] (get x :timestamp)) (flatten components))]
    (walk-intervals sorted-components (set []) [])))
