(ns crawljer.core
  (:require [clojure.core.async :as async]
            [clj-http.client :as client]
            [net.cgrand.enlive-html :as parser]
            [clj-http.client :as http]
            [clojure.java.io :as io]
            [me.raynes.fs :as fs]))

;; Channels for the URLs and the DOCs retrieved from thos URLs
(def urls-chan (async/chan Integer/MAX_VALUE))
(def docs-chan (async/chan Integer/MAX_VALUE))
(def ^:dynamic *folder* (System/getProperty "user.home"))

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
  (doall (map add-url coll))
  true)

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
  [current-url]
  (-> current-url
      (get-page-from-url)
      (get-urls-from-page)
      (add-urls-to-chan)))

(defn read-urls
  []
  (async/go-loop [current-url (async/<! urls-chan)]
    (when (read-url current-url)
      (recur (async/<! urls-chan)))))

(defn get-filename-from-url
  [s]
  (->> s
       (#(clojure.string/split % #"/"))
       (last)))

(defn get-prefixed-name-from-filename
  [s]
  (clojure.string/join "/" [*folder* s]))

(defn write-doc
  [h]
  (-> h
       (#(% :trace-redirects))
       (first)
       (get-filename-from-url)
       (get-prefixed-name-from-filename)
       (fs/normalized)
       (#(spit % (h :body))))
  true)

(defn write-docs
  []
  (async/go-loop [doc (async/<! docs-chan)]
    (when (write-doc doc)
      (recur (async/<! docs-chan)))))

;; Starts the crawl
(defn crawl
  []
  (read-urls)
  (write-docs))

;; Process URLs from the urls-chan
(defn process-urls
  "Process the urls in the urls-chan"
  [coll]
  (crawl)
  (add-urls-to-chan coll))

;; create directory tree given a path
(defn create-directory
  [s]
  (let [dir s]
    (when-not (fs/exists? dir)
      (fs/mkdirs dir)
      (def ^:dynamic *folder* s)
      dir)))

(defn start
  "Crawls the given vector of urls and saves the documents in the given path
  - coll : the vector of urls
  - s : the path to write to
  "
  [coll s]
  (create-directory s)
  (process-urls coll))

(defn -main
  [& args]
  (println "hello world")
  (start ["http://example.com"] "/home/pawandubey/crawljer"))
