(ns double-booked.core
  (:require [clj-time.coerce :as c]
            [clojure.core.reducers :as reducers]
            [clojure.math.combinatorics :as combo]))

(defrecord Event [name start end])

(defn make-event [name start end]
  (->Event name (c/to-long start) (c/to-long end)))

(defrecord EventConflict [name1 name2])

(defn make-conflict [event-names]
  (->EventConflict (first event-names) (second event-names)))

(defrecord EventComponent [timestamp type name])

(defn event-to-components [e]
  [(->EventComponent (get e :start) :start (get e :name))
   (->EventComponent (get e :end) :end (get e :name))])

; Define

(def t1 (make-event "My Birthweek" "1995-05-31" "1995-06-07"))
(def t2 (make-event "My Birthday" "1995-05-31" "1995-06-01"))
(def t3 (make-event "First of June" "1995-06-01" "1995-06-02"))
(def t4 (make-event "Winter Break" "1995-12-22" "1996-01-05"))
(def t5 (make-event "Christmas" "1995-12-25" "1995-12-26"))


; overlap? is a simple predicate that performs a series of boolean operations
; to determine whether or not two events overlap, based off their start
; and end timestamps
(defn overlap? [event-pair]
  (or  (and (<= (get (first event-pair) :start) (get (second event-pair) :end))
            (>= (get (first event-pair) :end) (get (second event-pair) :start)))
       (and (<= (get (second event-pair) :start) (get (first event-pair) :end))
            (>= (get (second event-pair) :end) (get (first event-pair) :start)))))


; get-conflicts-naive is the naive implementation of this problem
; essentially, compare each event to all other events, checking for overlaps.
; this approach is extremely lightweight, but it ends up being slow, due to
; it's time complexity of O(n^2)
(defn get-conflicts-naive [list-of-events]
  (let [event-pairs (combo/combinations list-of-events 2)]
    (filter overlap? event-pairs)))

; walk-intervals performs accumulative recursion through the sorted
; list of event components, collecting conflicts whenever a new start component
; is seen before an event is closed off by it's end component.
;
; components are created by first generating all possible pairs of event
; names in the 'active' set, and mapping the make-conflict constructor on them.
; these conflicts are added to the conflicts sequence, which is returned upon
; running out of event components.
(defn walk-intervals [components active conflicts]
  (cond
    (empty? components) conflicts
    (= :start (get (first components) :type))
     (let [next-active (conj active (get (first components) :name))
           next-conflicts (conj conflicts (map make-conflict (combo/combinations next-active 2)))]
       (walk-intervals (rest components) next-active next-conflicts))
    (= :end (get (first components) :type))
     (let [next-active (disj active  (get (first components) :name))]
       (walk-intervals (rest components) next-active conflicts))))

; get-conflicts-intervals splits the events into their start and end
; components, and sorts them. once sorted, it returns all events that have
; starts within other events. looking for nested ends is redundant.
;
; event components are sorted by timestamp, which is represented by the
; unix timestamp (milliseconds since epoch).
;
; this approach is must faster that the naive approach, and since it only
; requires one scan of the events after sorting, the time complexity of
; this solution is O(nlogn)
(defn get-conflicts-intervals [list-of-events]
  (let [components (map event-to-components list-of-events)
        sorted-components (sort-by (fn [x] (get x :timestamp)) (flatten components))]
    (walk-intervals sorted-components (set []) [])))
