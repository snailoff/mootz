(ns mootz.core
  (:gen-class)
  (:require [mootz.extensions :as ext]
            [mootz.util :as util]
            [base64-clj.core :as base64]
            [clj-time.format :as timef]
            [clj-time.coerce :as timec]
            [clojure.edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [compojure.core :refer :all]
            [compojure.route :as route])

  (:use ring.adapter.jetty))

(def config (clojure.edn/read-string (slurp "config.edn")))

(def index-file (slurp (str "resources/themes/" (:theme config) "/index.html")))

(defn full-path [path]
  (str "resources/public/root/" path))

(defn date-string [path]
  (if (.exists (io/file path))
    (let [date (.lastModified (io/file path))]
      (-> (timef/unparse (timef/formatter "yyyyMMdd hhmmss") (timec/from-long date))
          (str/replace #"0" "o")))
    "oooooooo oooooo"))


(defn apply-extensions [content]
  (-> content
      (ext/markdown)
      (ext/world)
      ))

(defn depth [path]
  (let [cpath (if (= path ".") path (str "./" path))]
    (str/join "<br />"
              (map #(str "<a href=\"" %1 "\">"
                         (str/replace %1 #"^.*/" "")
                         "</a>")
                   (loop [p path
                          result []]
                     (if (= p "")
                       (reverse result)
                       (recur (str/replace p #"/?[^/]*?$" "")
                              (conj result p))))
    ))))

(defn file-list [path]
  (str (str/join "<br />"
                 (map #(str "<a href=\"#\">"
                            (str/replace %1 #"^.*/" "")
                            "</a>")
                      (filter #(.isDirectory %)
                              (.listFiles (io/file (full-path path))))))
       "<br />"
       (str/join "<br />"
                 (map #(str "- <a href=\"#\">"
                            (str/replace %1 #"^.*/" "")
                            "</a>")
                      (filter #(.endsWith (.getName %) ".mz")
                      (.listFiles (io/file (full-path path))))))))

(defn parse-directory [path]
  {:isdir true
   :depth (depth path)
   :list (file-list path)
   :name (if (= path ".") "/"
             (str/replace path #".*/" ""))
   :content (apply-extensions (util/slurp-exists (full-path (str path "/_"))))
   :date (date-string (full-path path))
   })

(defn parse-file [path]
  {:isdir false
   :depth (depth (str/replace path #"/?[^/]*?.mz$" ""))
   :list (file-list (str/replace path #"/?[^/]*?.mz$" ""))
   :name (str/replace path #"^.*/|.mz$" "")
   :content (apply-extensions (slurp (full-path path)))
   :date (date-string (full-path path))
   })

(defn parse-request [path]
  (if (.exists (io/file (full-path path)))
    (if (.isDirectory (io/file (full-path path)))
      (parse-directory path)
      (parse-file path))

    (parse-directory ".")))


(defn decode-path [encoded]
  (try (let [decoded (base64/decode (str/replace encoded #"^/+|/+$" ""))]
         (if (= decoded "")
           "."
           decoded)
         )
       (catch Exception e "exception")))

(defn render [request]
  (if (some? (re-matches #"favicon.ico" (:uri request)))
    ""
;    (let [pinfo (parse-request (decode-path (:uri request)))]
     (let [pinfo (parse-request (str/replace (:uri request) #"^/*|/*$" ""))]

      ;왜 안되지.
      ;(assoc pinfo :content (apply-extensions pinfo))

      (-> index-file
          (str/replace #"__PAGE_NAME__" (:name pinfo))
          (str/replace #"__PAGE_CONTENT__" (:content pinfo))
          (str/replace #"__PAGE_DATE__" (:date pinfo))
          (str/replace #"__DEPTH__" (:depth pinfo))
          (str/replace #"__LIST__" (:list pinfo))
          ))))

(defroutes app
  (GET "/*" request (render request))
  (route/resources "/")
  (route/not-found "<h1>Page not found</h1>"))


(defn -main
  [& args] (run-jetty app {:port 3000}))

