(ns mootz
  (:gen-class)
  (:require [clojure.edn]
            [hawk.core :as hawk])
  (:use ring.adapter.jetty))

(def config (clojure.edn/read-string (slurp "config.edn")))

(defn handler [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "hi ring."})

(defn -main
  [& args]

  (println "hi mootz.")
  (println (:rootpath config))
  (hawk/watch! [{:paths [(:rootpath config)]
                 :handler (fn [ctx e]
                            (println "event: "
                                     (.getAbsolutePath (:file e))
                                     (:kind e))
                            (println "context: "
                                     ctx)
                            ctx)}])

  (run-jetty handler {:port 3000}))

