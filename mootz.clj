(ns mootz
  (:gen-class)
  (:require [clojure.edn]
            [clojure.java.io :as io]
            [clojure.string :refer [replace]]
            [hawk.core :as hawk])
  (:use ring.adapter.jetty))

(def config (clojure.edn/read-string (slurp "config.edn")))

(def index-file
  (slurp (str "themes/" (:theme config) "/index.html")))

(defn special-file [name]
  (let [path (str (:rootpath config) "/" name)]
    (if (.exists (io/file path))
      (slurp path)
      "")))


(defn render [request]
  (let [content index-file]
    (-> content
        (replace #"__MAIN_NAME__" (special-file "_main_name"))
        (replace #"__HEADER__" (special-file "_header"))
        (replace #"__FOOTER__" (special-file "_footer")))))

(defn handler [request]
  (println (:uri request))
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

  (run-jetty handler {:port 3000}))

