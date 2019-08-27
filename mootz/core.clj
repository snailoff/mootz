(ns mootz.core
  (:gen-class)
  (:require [mootz.extensions :as ext]
            [mootz.util :as util]
            [base64-clj.core :as base64]
            [clojure.edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [compojure.core :refer :all]
            [compojure.route :as route])

  (:use ring.adapter.jetty))

(def config (clojure.edn/read-string (slurp "config.edn")))

(def index-file (slurp (str "resources/themes/" (:theme config) "/index.html")))

(defn full-path [path]
  (str (str/replace (:rootpath config) #"/+$" "")
         (str "/" path)))

(defn apply-extensions [content]
  (-> content
      (ext/markdown)
      (ext/world)
      ))

(defn current-pagepath [path]
    (str/join "<br />"
          (map #(str "<b>" %1 "</b>")
               (str/split path #"/"))))

(defn parse-directory [path]
  {:isdir true
   :path path
   :name (if (= path "") (:rootname config)
             (str/replace path #".*/" ""))
   :content (apply-extensions (util/slurp-exists (full-path (str path "/_"))))
   :date (.lastModified (io/file (full-path path)))
   })

(defn parse-file [path]
  {:isdir false
   :path path
   :name (str/replace path #"^.*/|.mz$" "")
   :content (apply-extensions (slurp (full-path path)))
   :date (.lastModified (io/file (full-path path)))
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
    (let [pinfo (parse-request (decode-path (:uri request)))]

      ;왜 안되지.
      ;(assoc pinfo :content (apply-extensions pinfo))

      (-> index-file
          (str/replace #"__PAGE_NAME__" (str "<h3>" (:name pinfo)"</h3>"))
          (str/replace #"__PAGE_CONTENT__" (str "<p>" (:content pinfo)"</p>"))
          (str/replace #"__PAGE_DATE__" (str "<small>" (:date pinfo) "</small>"))
          (str/replace #"__PATH__" (:path pinfo))
          ))))

(defroutes app
  (GET "/*" request (render request))
  (route/resources "/")
  (route/not-found "<h1>Page not found</h1>"))


(defn -main
  [& args] (run-jetty app {:port 3000}))

