(ns double-booked.core-test
  (:require [clojure.test :refer :all]
            [double-booked.core :refer :all]))

(def t1 (make-event "My Birthweek" "1995-05-31" "1995-06-07"))
(def t2 (make-event "My Birthday" "1995-05-31" "1995-06-01"))
(def t3 (make-event "First of June" "1995-06-01" "1995-06-02"))
(def t4 (make-event "Winter Break" "1995-12-22" "1996-01-05"))
(def t5 (make-event "Christmas" "1995-12-25" "1995-12-26"))
(def t6 (make-event "Nest1" "2017-05-01" "2017-05-31"))
(def t7 (make-event "Nest2" "2017-05-05" "2017-05-17"))
(def t8 (make-event "Nest3" "2017-05-07" "2017-05-18"))
(def t9 (make-event "Nest4" "2017-05-07" "2017-05-08"))
(def t10 (make-event "Nest5" "2017-05-07T12:00:04" "2017-05-07T12:04:04"))

; The results are converted to sets because both algorithms create the pairs
; in different orders. Thus, to prove they have the same output, converting
; all pairs to sets is necessary when checking for equality of event conflicts
(defn pairs-to-sets [pairs] (set (map (fn [x] (set x)) pairs)))

(def winter-break-overlap-sets #{#{"Winter Break" "Christmas"}})

(def nested-overlap-sets #{#{"Nest1" "Nest4"}
                           #{"Nest2" "Nest4"}
                           #{"Nest1" "Nest2"}
                           #{"Nest1" "Nest3"}
                           #{"Nest4" "Nest5"}
                           #{"Nest1" "Nest5"}
                           #{"Nest3" "Nest4"}
                           #{"Nest3" "Nest5"}
                           #{"Nest2" "Nest5"}
                           #{"Nest2" "Nest3"}})

(deftest winter-break-overlap-naive
  (testing "Winter break overlap with Christmas, naive algorithm"
    (is (= winter-break-overlap-sets
           (pairs-to-sets (get-conflicts-naive [t4 t5]))))))

(deftest winter-break-overlap-intervals
  (testing "Winter break overlap with Christmas, intervals algorithm"
    (is (= winter-break-overlap-sets
           (pairs-to-sets (get-conflicts-intervals [t4 t5]))))))

(deftest nesting-events-naive
  (testing "Five overlapping and increasingly shorter events, naive algorithm"
    (is (= nested-overlap-sets
           (pairs-to-sets (get-conflicts-naive [t6 t7 t8 t9 t10]))))))

(deftest nesting-events-intervals
  (testing "Five overlapping and increasingly shorter events, intervals algorithm"
    (is (= nested-overlap-sets
           (pairs-to-sets (get-conflicts-intervals [t6 t7 t8 t9 t10]))))))
