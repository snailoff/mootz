(ns mootz.core
  (:gen-class)
  (:require [mootz.extensions :as ext]
            [base64-clj.core :as base64]
            [clojure.edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [compojure.core :refer :all]
            [compojure.route :as route])

  (:use ring.adapter.jetty))

(def config (clojure.edn/read-string (slurp "config.edn")))

(defn index-file []
  (slurp (str "resources/themes/" (:theme config) "/index.html")))

(defn real-path [path]
    (str (:rootpath config)
         "/"
         path))

(defn current-pagepath [path]
    (str/join "<br />"
          (map #(str "<b>" %1 "</b>")
               (str/split path #"/"))))



(defn parse-directory [path]
  {:isdir true
   :path path
   :name (if (= path "") (:rootname config) (str/replace path #".*/" ""))
   :content (if (.exists (io/file (real-path (str path "/_"))))
              (slurp (real-path (str path "/_")))
              "")
   :date (.lastModified (io/file (real-path path)))
   })

(defn parse-file [path]
  {:isdir false
   :path path
   :name (str/replace path #"^.*/|.mz$" "")
   :content (slurp (real-path path))
   :date (.lastModified (io/file (real-path path)))
   })

(defn parse-request [uri]
  (try
    (let [decoded (base64/decode (replace uri #"^/" ""))]
      (if-not (.exists (io/file (real-path decoded))) (throw (Exception. "not exists")))

      (if (.isDirectory (io/file decoded))
        (parse-directory decoded)
        (parse-file decoded)))

    (catch Exception e
      (parse-directory ""))
  ))

(defn apply-extensions [pinfo]
  (-> (:content pinfo)
      (ext/markdown)
      (ext/world)
      ))

(defn render [request]
  (if (some? (re-matches #"favicon.ico" (:uri request)))
    ""
    (let [template (index-file)
          pinfo (parse-request (:uri request))]

      ;왜 안되지.
      ;(assoc pinfo :content (apply-extensions pinfo))

      (-> template
          (str/replace #"__PAGE_NAME__" (str "<h3>" (:name pinfo)"</h3>"))
          (str/replace #"__PAGE_CONTENT__" (str "<p>" (apply-extensions pinfo)"</p>"))
          (str/replace #"__PAGE_DATE__" (str "<small>" (:date pinfo) "</small>"))
          (str/replace #"__PATH__" (:path pinfo))
          ))))

(defroutes app
  (GET "/" request (render request))
  (route/resources "/")
  (route/not-found "<h1>Page not found</h1>"))


(defn -main
  [& args] (run-jetty app {:port 3000}))

