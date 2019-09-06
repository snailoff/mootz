(ns mootz.util
  (:require [clojure.java.io :as io]))

(defn slurp-exists [path]
  (if (.exists (io/file path))
    (slurp path)
    ""
    ))

(defn full-path [path]
  (str "resources/public/root/" path))
