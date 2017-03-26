(ns double-booked.core
  (:require [clj-time.coerce :as c]
            [clojure.core.reducers :as reducers]
            [clojure.math.combinatorics :as combo]))

(defrecord Event [name start end])

(defn make-event [name start end]
  (->Event name (/ (c/to-long start) 60000) (/ (c/to-long end) 60000)))

(defrecord EventConflict [name1 name2])

(defn make-conflict [event-names]
  (->EventConflict (first event-names) (second event-names)))

(def a (Event. "My Birthday" "05-31-95T00:00:00" "05-31-95T23:59:59"))

; to-time-maps
; this is where we do a list comprehension for each event, creating an entry
; for each time slot, and convert that into a map.
(defn to-time-maps [e]
  (into (hash-map) (let [name   (get e :name)
                         start  (get e :start)
                         end    (get e :end)]
                     (for [time (range start (+ end 1))]
                       [time [name]]))))

; combinef
; this is supplied to the fold in get-calendar-map, and simply applies
; merge-with for each map in the list of maps returned from get-time-maps.
; could be declared as a lambda with the (fn []()) notation but it was
; taken out for readability
(defn combinef ([xs x] (merge-with into x xs)) ([] {}))

; get-calendar-map
; in this procedure, all the time-maps are merged using merge-with.
; the heuristic here is that if there is a time conflict, conflicting Events
; are recorded in a list corresponding to the time stamp in which they overlap
;
; NOTE: order may be lost at this point, but it no longer matters, since we
; have already recorded all conflicts and have the correct date ranges
; represented in the hash-map.
(defn get-calendar-map [list-of-events]
  (reducers/fold combinef (map to-time-maps list-of-events)))


; collect-conflicts
; this procedure goes through the merged calendar map, and collects all of the
; time slots that have conflicts, and create EventConflict objects out of
; all of them.
;
; it is essentially a one to many map of calendar map entries to conflicts.
(defn collect-conflicts [list-of-events]
  (let [conflicting-events  (filter
                              (fn [x] (> (count (second x)) 1))
                              (seq (get-calendar-map list-of-events)))
        conflict-pairs      (map
                              (fn [x] (map make-conflict (combo/combinations (second x) 2)))
                              conflicting-events)]
    (flatten conflict-pairs)))

; get-conflicts
; this is the only function that needs to be called from the outside
; it takes a sequence of Events, created using the double-booked.core.Event
; class, and returns a sequence containing all pairs of conflicting Events
; by their name.
;
(defn get-conflicts [list-of-events]
  (distinct (collect-conflicts list-of-events)))

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
