(ns mootz.core
  (:gen-class)
  (:require [mootz.extensions :as ext]
            [mootz.private :as prv]
            [base64-clj.core :as base64]
            [clj-time.format :as timef]
            [clj-time.coerce :as timec]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [compojure.core :refer :all]
            [compojure.route :as route])

  (:use ring.adapter.jetty
        ring.middleware.params))

(defn index-file [] (slurp (str "resources/template/index.html")))


(defn r-name [uri]
  (if (= uri "")
    "root"
    (string/replace uri #"^.*/" "")))

(defn r-depth [uri]
  (let [dir (if (prv/is-dir? uri)
              uri
              (string/replace uri #"/?[^/]*$" ""))]
    (string/join " - "
              (map #(if (= %1 "")
                      "<a href=\"/\"><b>root</b></a>"
                      (str "<a href=\""
                           %1
                           ".mz\"><b>"
                           (string/replace %1 #"^.*/" "")
                           "</b></a>"))
                   (loop [p dir
                          result []]
                     (if (= p "")
                       (reverse (conj result ""))
                       (recur (string/replace p #"/?[^/]*?$" "")
                              (conj result p))))
                   ))))

(defn r-list [uri]
  (let [dir (if (prv/is-dir? uri)
              uri
              (string/replace uri #"/?[^/]*$" ""))]
      (str (string/join " . "
                 (map #(str "<b><a href=\""
                             (string/replace %1 #"^.*/root/" "/")
                            ".mz\">"
                             (.getName %1)
                            "</a></b>")
                      (sort
                       (filter #(.isDirectory %)
                               (.listFiles (io/file (prv/full-path dir))))))
                       )
        "<br />"
        (string/join " . "
                  (map #(str " <a href=\""
                              (string/replace %1 #"^resources/public/root/" "/")
                             ".mz\">"
                             (string/replace (.getName %1) #".md$" "")
                            "</a>")
                       (filter #(not (or (.isDirectory %)
                                         (string/includes? % ".")
                                         (string/ends-with? % "/_")
                                         ))
                              (.listFiles (io/file (prv/full-path dir)))))))))

(defn r-content [uri]
  (let [path (if (prv/is-dir? uri) (str uri "/_") uri)
        content (prv/slurp-exists (prv/full-path path))]
    (-> content
        (ext/markdown)
        (ext/images uri))
    ))

(defn r-date [path]
  (if (.exists (io/file path))
    (let [date (.lastModified (io/file path))]
      (-> (timef/unparse (timef/formatter "yyyy.MM.dd. hh:mm:ss") (timec/from-long date))))
    "0000.00.00. 00:00:00"))

(defn render [rawuri]
  (let [uri (prv/valid-uri rawuri)]
    (-> (index-file)
        (string/replace #"__PAGE_NAME__" (r-name uri))
        (string/replace #"__DEPTH__" (r-depth uri))
        (string/replace #"__LIST__" (r-list uri))
        (string/replace #"__PAGE_CONTENT__" (r-content uri))
        (string/replace #"__PAGE_DATE__" (r-date (prv/full-path uri)))
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

