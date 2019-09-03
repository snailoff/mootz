(ns mootz.core
  (:gen-class)
  (:require [mootz.extensions :as ext]
            [mootz.util :as util]
            [base64-clj.core :as base64]
            [clj-time.format :as timef]
            [clj-time.coerce :as timec]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.util.codec :as codec])

  (:use ring.adapter.jetty
        ring.middleware.params))

(defn index-file [] (slurp (str "resources/template/index.html")))

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
  (let [cpath (if (= path ".")
                path
                (str "./" path))]
        (str/join " - "
                  (map #(if (= %1 "")
                          "<a href=\"/\"><b>root</b></a>"
                          (str "<a href=\""
                               %1
                               "\"><b>"
                               (str/replace %1 #"^.*/" "")
                               "</b></a>"))
                      (loop [p path
                              result []]
                        (if (= p "")
                          (reverse (conj result ""))
                          (recur (str/replace p #"/?[^/]*?$" "")
                                  (conj result p))))
                      ))))

(defn file-list [path]
  (str ". "
       (str/join " . "
                 (map #(str "<a href=\""
                             (str/replace %1 #"^.*/root/" "/")
                            "\">"
                             (.getName %1)
                            "</a>")
                      (filter #(.isDirectory %)
                              (.listFiles (io/file (full-path path))))))
        "<br />"
        ". "
        (str/join " . "
                  (map #(str " <a href=\""
                              (str/replace %1 #"^resources/public/root/" "/")
                             " \">"
                             (str/replace (.getName %1) #".mz$" "")
                            "</a>")
                      (filter #(.endsWith (.getName %) ".mz")
                              (.listFiles (io/file (full-path path))))))))

(defn parse-directory [uri]
  {:isdir true
   :depth (depth uri)
   :list (file-list uri)
   :name (if (= uri "") "root"
             (str/replace uri #".*/" ""))
   :content (apply-extensions (util/slurp-exists (full-path (str uri "/_"))))
   :date (date-string (full-path uri))
   })

(defn parse-file [uri]
  {:isdir false
   :depth (depth (str/replace uri #"/?[^/]*?.mz$" ""))
   :list (file-list (str/replace uri #"/?[^/]*?.mz$" ""))
   :name (str/replace uri #"^.*/|.mz$" "")
   :content (apply-extensions (slurp (full-path uri)))
   :date (date-string (full-path uri))
   })

(defn parse-request [uri]
  (if (.exists (io/file (full-path uri)))
    (if (.isDirectory (io/file (full-path uri)))
      (parse-directory uri)
      (parse-file uri))

    (parse-directory "")))

(defn render [uri]
  (let [pinfo (parse-request (str/replace (codec/url-decode uri) #"/*$" ""))]
    (-> (index-file)
        (str/replace #"__PAGE_NAME__" (:name pinfo))
        (str/replace #"__PAGE_CONTENT__" (:content pinfo))
        (str/replace #"__PAGE_DATE__" (:date pinfo))
        (str/replace #"__DEPTH__" (:depth pinfo))
        (str/replace #"__LIST__" (:list pinfo))
        )))

(defn action [request]
  (ring.util.response/redirect (:uri request))
  )

(defroutes app
   (GET "/favicon.ico" [] "ok")
   (POST "/*" request (action request))
   (GET "/*" request (render (:uri request)))
   (route/resources "/")
   (route/not-found "<h1>Page not found</h1>"))

(def app-handler
  (-> app wrap-params))


(defn -main
  [& args] (run-jetty (-> app wrap-params) {:port 3000}))

