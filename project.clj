(defproject rootm "0.1.0-SNAPSHOT"
  :dependencies [[base64-clj "0.1.1"]
                 [compojure "1.6.1"]
                 [markdown-clj "1.10.0"]
                 [org.clojure/clojure "1.10.0"]
                 [hawk "0.2.11"]
                 [ring "1.7.1"]
                 [ring/ring-jetty-adapter "1.6.3"]]

  :plugins [[lein-ring "0.12.5"]]

  :source-paths ["." "mootz"]
  :main mootz.core
  :aot [mootz.core]
  :clean-targets ^{:protect false} ["target"]

  :ring {:handler mootz.core/app}
  )

