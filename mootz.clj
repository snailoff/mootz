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

(defn mootz-path [uri]
  (if (= uri "/")
    ""
    (str (:rootpath config)
         "/"
         (base64/decode (replace uri #"^/" ""))
         ".mz")))

(defn current-pagecontent [uri]
  (println "uri: " uri)
  (cond (.exists (io/file (mootz-path uri))) (slurp (mootz-path uri))
        :else "no content"
        ))

(defn current-pagename [uri]
  (if (= uri "/")
    "no name"
    (->
      (base64/decode (replace uri #"^/" ""))
      (replace #".*/" "")
    )))

(defn current-pagedate [uri]
  (if (= uri "/")
    "oooooooo oooooo"
    (.lastModified (io/file
                   (mootz-path uri)))))

(defn current-pagepath [uri]
  (join "<br />"
        (map #(str "<b>" %1 "</b>")
             (split (base64/decode (clojure.string/replace uri #"^/" "")) #"/"))

   ))

(defn render [request]
  (let [template (index-file)
        pagecontent (current-pagecontent (:uri request))
        pagename (current-pagename (:uri request))
        pagedate (current-pagedate (:uri request))
        pagepath (current-pagepath (:uri request))]

    (-> template
        (replace #"__PAGE_NAME__" (str "<h3>" pagename "</h3>"))
        (replace #"__PAGE_CONTENT__" (str "<p>" pagecontent "</p>"))
        (replace #"__PAGE_DATE__" (str "<small>" pagedate "</small>"))
        (replace #"__PATH__" pagepath)
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

