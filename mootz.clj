(ns mootz
  (:gen-class)
  (:require [clojure.edn]
            [hawk.core :as hawk])
  (:use ring.adapter.jetty))

(def config (clojure.edn/read-string (slurp "config.edn")))

(defn handler [request]
  (println (:uri request))
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (slurp (str "themes/" (:theme config) "/index.html"))})

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

