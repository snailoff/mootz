(ns mootz
  (:gen-class)
  (:require [base64-clj.core :as base64]
            [clojure.edn]
            [clojure.java.io :as io]
            [clojure.string :refer :all]
            [hawk.core :as hawk])
  (:use ring.adapter.jetty))

(def config (clojure.edn/read-string (slurp "config.edn")))

(defn index-file []
  (slurp (str "resources/themes/" (:theme config) "/index.html")))

(defn special-file [name]
  (let [path (str (:rootpath config) "/" name)]
    (if (.exists (io/file path))
      (slurp path)
      "")))

(defn mootz-path [path]
    (str (:rootpath config)
         "/"
         path
         ".mz"))

(defn current-pagecontent [path]
  (cond (.exists (io/file (mootz-path path))) (slurp (mootz-path path))
        :else "no content"
        ))

(defn current-pagename [path]
    (replace path #".*/" ""))

(defn current-pagedate [path]
    (.lastModified (io/file
                   (mootz-path path))))

(defn current-pagepath [path]
    (join "<br />"
          (map #(str "<b>" %1 "</b>")
               (split path #"/"))))

(defn decode-path [uri]
  (if (= uri "/")
    ""
    (base64/decode
     (replace uri #"^/" ""))
    ))


(defn render [request]
  (println "decode-path : " (decode-path (:uri request)))
  (let [template (index-file)
        dpath (decode-path (:uri request))]

    (-> template
        (replace #"__PAGE_NAME__" (str "<h3>" (current-pagename dpath)"</h3>"))
        (replace #"__PAGE_CONTENT__" (str "<p>" (current-pagecontent dpath)"</p>"))
        (replace #"__PAGE_DATE__" (str "<small>" (current-pagedate dpath) "</small>"))
        (replace #"__PATH__" (current-pagepath dpath))
        )))

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

