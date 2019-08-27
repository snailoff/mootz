(ns mootz.extensions
  (:require [clojure.string :as str])
  (:use markdown.core))

(defn markdown [content]
  (md-to-html-string content))

(defn world [content]
  (str/replace content
               #"@world (.*?)@"
               (str "'hi " "$1" " world'")))

