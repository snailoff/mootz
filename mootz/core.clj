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

(defn date-string [path]
  (if (.exists (io/file path))
    (let [date (.lastModified (io/file path))]
      (-> (timef/unparse (timef/formatter "yyyyMMdd hhmmss") (timec/from-long date))))
    "00000000 000000"))



(defn depth [path]
  (let [cpath (if (= path ".")
                path
                (str "./" path))]
        (str/join " - "
                  (map #(if (= %1 "")
                          "<a href=\"/\"><b>root</b></a>"
                          (str "<a href=\""
                               %1
                               ".mz\"><b>"
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
  (str (str/join " . "
                 (map #(str "<b><a href=\""
                             (str/replace %1 #"^.*/root/" "/")
                            ".mz\">"
                             (.getName %1)
                            "</a></b>")
                      (sort
                       (filter #(.isDirectory %)
                               (.listFiles (io/file (util/full-path path))))))
                       )
        "<br />"
        (str/join " . "
                  (map #(str " <a href=\""
                              (str/replace %1 #"^resources/public/root/" "/")
                             ".mz\">"
                             (str/replace (.getName %1) #".md$" "")
                            "</a>")
                       (filter #(not (or (.isDirectory %)
                                         (str/includes? % ".")
                                         (str/ends-with? % "/_")
                                         ))
                              (.listFiles (io/file (util/full-path path))))))))

(defn get-content [uri]
  (let [content (util/slurp-exists (util/full-path uri))]
    (-> content
        (ext/markdown)
        (ext/world)
        (ext/images uri))
    ))

(defn parse-directory [uri]
  {:depth (depth uri)
   :list (file-list uri)
   :name (if (= uri "") "root"
             (str/replace uri #".*/" ""))
   :content (get-content (str uri "/_"))
   :date (date-string (util/full-path uri))
   })

(defn parse-file [uri]
  {:depth (depth (str/replace uri #"/?[^/]*$" ""))
   :list (file-list (str/replace uri #"/?[^/]*?$" ""))
   :name (str/replace uri #"^.*/" "")
   :content (get-content uri)
   :date (date-string (util/full-path uri))
   })

(defn parse-request [uri]
  (if (.exists (io/file (util/full-path uri)))
    (if (.isDirectory (io/file (util/full-path uri)))
      (parse-directory uri)
      (parse-file uri))

    (parse-directory "")))

(defn render [rawuri]
  (let [uri (str/replace (codec/url-decode rawuri) #".mz$" "")
        pinfo (parse-request uri)]
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
  (GET "/" request (render ""))
  (GET "/*.mz" request (render (:uri request)))
  (route/resources "/" {:root "/public/root"})
  (route/files "/" {:root "/public/root"})
  (route/not-found "<h1>Page not found</h1>"))

(def app-handler
  (-> app wrap-params))


(defn -main
  [& args] (run-jetty (-> app wrap-params) {:port 3000}))

