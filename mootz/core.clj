(ns mootz.core
  (:gen-class)
  (:require [mootz.extensions :as ext]
            [base64-clj.core :as base64]
            [clojure.edn]
            [clojure.java.io :as io]
            [clojure.string :refer :all]
            [hawk.core :as hawk]
            )

  (:use ring.adapter.jetty))

(def config (clojure.edn/read-string (slurp "config.edn")))

(defn index-file []
  (slurp (str "resources/themes/" (:theme config) "/index.html")))

(defn real-path [path]
    (str (:rootpath config)
         "/"
         path))

(defn current-pagepath [path]
    (join "<br />"
          (map #(str "<b>" %1 "</b>")
               (split path #"/"))))



(defn parse-directory [path]
  {:isdir true
   :path path
   :name (if (= path "") (:rootname config) (replace path #".*/" ""))
   :content (if (.exists (io/file (real-path (str path "/_"))))
              (slurp (real-path (str path "/_")))
              "")
   :date (.lastModified (io/file (real-path path)))
   })

(defn parse-file [path]
  {:isdir false
   :path path
   :name (replace path #"^.*/|.mz$" "")
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
          (replace #"__PAGE_NAME__" (str "<h3>" (:name pinfo)"</h3>"))
          (replace #"__PAGE_CONTENT__" (str "<p>" (apply-extensions pinfo)"</p>"))
          (replace #"__PAGE_DATE__" (str "<small>" (:date pinfo) "</small>"))
          (replace #"__PATH__" (:path pinfo))
          ))))

(defn handler [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
      :body (render request)})

(defn -main
  [& args]
  (hawk/watch! [{:paths [(:rootpath config)]
                 :handler (fn [ctx e]
                            (println "event: "
                                     (.getAbsolutePath (:file e))
                                     (:kind e)
                                     ctx)
                            ctx)}])

;;  (run-jetty handler {:port 3000})
  )
