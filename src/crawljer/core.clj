(ns crawljer.core
  (:require [clojure.core.async :as async]
            [clj-http.client :as client]
            [net.cgrand.enlive-html :as parser]
            [clj-http.client :as http]))

;; Channels for the URLs and the DOCs retrieved from thos URLs
(def urls-chan (async/chan Integer/MAX_VALUE))
(def docs-chan (async/chan Integer/MAX_VALUE))

;; URL pattern to extract URLs from hrefs
(def url-pattern #"([\\]+\")?(((?!\\+).)*)([\\]+\")?")

;; Keep track of the URLs seen already
(def visited-urls (atom {}))

;; Test if the current resource is an html document
(defn is-html?
  [page]
  (clojure.string/includes? ((:headers page) "Content-Type") "text/html"))

;; Only consider links with the http protocol
(defn valid-url?
  [s]
  (not= nil (re-find #"^https?://" s)))

;; Add URL to urls-chan if not visited already
;; and mark it as visited
(defn add-url
  [s]
  (when-not (@visited-urls s)
    (swap! visited-urls assoc s true)
    (async/put! urls-chan s)))

;; Add all URLs to urls-chan
(defn add-urls-to-chan
  [coll]
  (map add-url coll))

(defn get-page-from-url
  [s]
  (let [page (http/get s)]
    (when (is-html? page)
      (async/put! docs-chan (:body page))
      page)))

(defn get-urls-from-page
  [page]
  (let [snippet (parser/html-snippet page)]
    (if snippet
      (->> (parser/select snippet [:a])
           (map #(:href (:attrs %)))
           (map #((re-find url-pattern %) 2))
           (#(vec (filter valid-url? %)))))))

(defn read-url
  []
  (let [current-url (async/<! urls-chan)]
    (-> current-url
        (get-page-from-url)
        (get-urls-from-page)
        (add-urls-to-chan))))

(defn read-urls
  []
  (async/go-loop []
    (when read-url
      (recur))))

(defn write-doc
  []
  (println (async/<! docs-chan))
  true
  )

(defn write-docs
  []
  (async/go-loop []
    (when write-doc
      (recur))))

;; Starts the crawl
(defn crawl
  []
  (read-urls)
  (write-docs))

;; Process URLs from the urls-chan
(defn process-urls
  "Process the urls in the urls-chan"
  [coll]
  (add-urls-to-chan coll)
  (crawl))

(defn start
  "Crawls the given vector of urls and saves the documents in the given path
  - coll : the vector of urls
  - s : the path to write to
  "
  [coll s]
  (process-urls coll))
