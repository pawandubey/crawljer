(ns crawljer.core-test
  (:require [clojure.test :refer :all]
            [crawljer.core :refer :all]
            [clj-http.client :as http]))

(deftest content-type-test
  (testing "Content type should be text/html"
    (let [doc (http/get "http://localhost")]
      (is (is-html? doc)))
    (let [doc (http/get "http://localhost/icons/ubuntu-logo.png")]
      (is (not (is-html? doc))))))

