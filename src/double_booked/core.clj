(ns double-booked.core
  (:require [clj-time.coerce :as c]
            [clojure.math.combinatorics :as combo]
            [clojure.set :as set]))

(defrecord Event [name start end])

(defn make-event [name start end]
  (->Event name (c/to-long start) (c/to-long end)))

(defrecord EventComponent [timestamp type name])

(defn event-to-components [e]
  [(->EventComponent (get e :start) :start (get e :name))
   (->EventComponent (get e :end) :end (get e :name))])

; overlap? is a simple predicate that performs a series of boolean operations
; to determine whether or not two events overlap, based off their start
; and end timestamps
(defn overlap? [event-pair]
  (or  (and (< (get (first event-pair) :start) (get (second event-pair) :end))
            (> (get (first event-pair) :end) (get (second event-pair) :start)))
       (and (< (get (second event-pair) :start) (get (first event-pair) :end))
            (> (get (second event-pair) :end) (get (first event-pair) :start)))))


; get-conflicts-naive is the naive implementation of this problem.
; essentially, compare each event to all other events, checking for overlaps.
; using the clojure combinatorics library, generate all possible event pairs,
; and filter through them using the predicate 'overlap?' defined above.
; this approach is fairly lightweight code-wise, but it ends up being slow,
; due to it's time complexity of O(n^2)
(defn get-conflicts-naive [list-of-events]
  (let [event-pairs (combo/combinations list-of-events 2)]
    (set (map (fn [x] (seq [(get (first x) :name) (get (second x) :name)]))
              (filter overlap? event-pairs)))))

; walk-intervals performs accumulative recursion through the sorted
; list of event components, collecting conflicts whenever a new start component
; is seen before an event is closed off by its end component.
;
; for every start component, new conflicts are potentially accumulated.
; this is done by a set union of all pairs of newly 'active' events
; (meaning their start has been seen, but not their end) and the previously
; recorded conflicts. we use union here to ensure no duplicates are recorded.
;
; active events are stored in a set, and each time the end component of an
; event is seen, the next recursive call uses the active set disjoint with
; the recently ended event.
(defn walk-intervals [components active conflicts]
  (cond
    (empty? components) conflicts
    (= :start (get (first components) :type))
     (let [next-active (conj active (get (first components) :name))
           next-conflicts (set/union conflicts (set (combo/combinations next-active 2)))]
       (walk-intervals (rest components) next-active next-conflicts))
    (= :end (get (first components) :type))
     (let [next-active (disj active  (get (first components) :name))]
       (walk-intervals (rest components) next-active conflicts))))

; get-conflicts-intervals splits the events into their start and end
; components, and sorts them. once sorted, it walks through the new list of
; intervals, and records conflicts for all starts that occur before previous
; events have ended.
;
; event components are sorted by timestamp, which is represented by the
; unix timestamp (milliseconds since epoch), calculated in the Event constructor
;
; this approach is must faster than the naive approach, and since it only
; requires one scan of the events after sorting, the time complexity of
; this solution is O(nlogn)
(defn get-conflicts-intervals [list-of-events]
  (let [components (map event-to-components list-of-events)
        sorted-components (sort-by (fn [x] (get x :timestamp)) (flatten components))]
    (walk-intervals sorted-components #{} #{})))
