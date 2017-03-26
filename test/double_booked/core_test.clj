(ns double-booked.core-test
  (:require [clojure.test :refer :all]
            [double-booked.core :refer :all])
  (:import  [double_booked.core Event]))


(deftest is-birthday
  (testing "Is my birthday entered correctly?"
    (is (= a (Event. "My Birthday" "05-31-95T00:00:00" "05-31-95T23:59:59")))))
