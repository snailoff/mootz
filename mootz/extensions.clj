(ns mootz.extensions
  (:use markdown.core))

(defn markdown [content]
  (md-to-html-string content))

(defn world [content]
  (clojure.string/replace content
                          #"@world (.*?)@"
                          (str "'hi " "$1" " world'")))

