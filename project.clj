(defproject rootm "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [hawk "0.2.11"]
                 [ring "1.7.1"]
                 [ring/ring-jetty-adapter "1.6.3"]]


  :source-paths ["."]
  :main mootz
  :aot [mootz]
  :clean-targets ^{:protect false} ["target"]
  )

