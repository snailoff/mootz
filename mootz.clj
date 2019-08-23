(ns mootz
  (:require [clojure.edn]
            [hawk.core :as hawk]))

(def config (clojure.edn/read-string (slurp "config.edn")))

(defn -main
  [& args]
  (println "hi mootz.")
  (println (:rootpath config))
  (hawk/watch! [{:paths [(:rootpath config)]
                 :handler (fn [ctx e]
                            (println "event: " (.getAbsolutePath (:file e)) (:kind e))
                            (println "context: " ctx)
                            ctx)}]))
