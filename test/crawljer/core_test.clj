(ns crawljer.core-test
  (:require [clojure.test :refer :all]
            [crawljer.core :refer :all]
            [clj-http.client :as http]))

(def mock-page {:status 200, :headers {"X-Cache" "HIT", "Server" "ECS (ewr/15BD)", "Content-Type" "text/html", "Content-Length" "606", "Connection" "close", "Accept-Ranges" "bytes", "Expires" "Fri, 31 Mar 2017 22:42:57 GMT", "Etag" "\"359670651+gzip\"", "Date" "Fri, 24 Mar 2017 22:42:57 GMT", "Vary" "Accept-Encoding", "Last-Modified" "Fri, 09 Aug 2013 23:54:35 GMT", "Cache-Control" "max-age=604800"}, :body "<!doctype html>\n<html>\n<head>\n    <title>Example Domain</title>\n\n    <meta charset=\"utf-8\" />\n    <meta http-equiv=\"Content-type\" content=\"text/html; charset=utf-8\" />\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />\n    <style type=\"text/css\">\n    body {\n        background-color: #f0f0f2;\n        margin: 0;\n        padding: 0;\n        font-family: \"Open Sans\", \"Helvetica Neue\", Helvetica, Arial, sans-serif;\n        \n    }\n    div {\n        width: 600px;\n        margin: 5em auto;\n        padding: 50px;\n        background-color: #fff;\n        border-radius: 1em;\n    }\n    a:link, a:visited {\n        color: #38488f;\n        text-decoration: none;\n    }\n    @media (max-width: 700px) {\n        body {\n            background-color: #fff;\n        }\n        div {\n            width: auto;\n            margin: 0 auto;\n            border-radius: 0;\n            padding: 1em;\n        }\n    }\n    </style>    \n</head>\n\n<body>\n<div>\n    <h1>Example Domain</h1>\n    <p>This domain is established to be used for illustrative examples in documents. You may use this\n    domain in examples without prior coordination or asking for permission.</p>\n    <p><a href=\"http://www.iana.org/domains/example\">More information...</a></p>\n</div>\n</body>\n</html>\n", :request-time 442, :trace-redirects ["http://example.com"], :orig-content-encoding "gzip"})

(def mock-urls ["http://www.iana.org/domains/example"])

(deftest valid-url-test
  (testing "It should return true for valid urls"
    (let [valid-urls ["http://localhost" "https://example.com"]]
      (is (= [true true] (vec (map valid-url? valid-urls))))))
  (testing "It should return false for invalid urls"
    (let [invalid-urls ["htt://localhost" "ww-w.heelobar"]]
      (is (= [false false] (vec (map valid-url? invalid-urls)))))))

(deftest content-type-test
  (testing "Content type should be text/html"
    (let [doc (http/get "http://localhost")]
      (is (is-html? doc)))
    (let [doc (http/get "http://localhost/icons/ubuntu-logo.png")]
      (is (not (is-html? doc))))))

(deftest get-page-from-url-test
  (testing "Should return the page from the URL"
    (let [url "http://example.com"]
      (is (= (mock-page :body) ((get-page-from-url url) :body))))))

(deftest get-urls-from-page-test
  (testing "Should properly retreive all URLs from the page"
    (let [html-page mock-page]
      (is (= mock-urls (get-urls-from-page html-page))))))
