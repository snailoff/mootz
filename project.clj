(defproject rootm "0.1.0-SNAPSHOT"
  :dependencies [[base64-clj "0.1.1"]
                 [org.clojure/clojure "1.10.0"]
                 [hawk "0.2.11"]
                 [ring "1.7.1"]
                 [ring/ring-jetty-adapter "1.6.3"]]

  :plugins [[lein-ring "0.12.5"]]

  :source-paths ["."]
  :main mootz
  :aot [mootz]
  :clean-targets ^{:protect false} ["target"]

  :ring {:handler mootz/handler}
  )

