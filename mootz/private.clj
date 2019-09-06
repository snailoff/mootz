(ns mootz.private
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [ring.util.codec :as codec]))

(defn slurp-exists [path]
  (if (.exists (io/file path))
    (slurp path)
    ""
    ))

(defn full-path [path]
  (str "resources/public/root/" path))


(defn is-dir? [uri]
  (.isDirectory (io/file (full-path uri))))

(defn valid-uri [rawuri]
  (let [uri (string/replace (codec/url-decode rawuri) #".mz$" "")
        res (io/file (full-path uri))]
    (if (= (.exists res) true) uri "")))

