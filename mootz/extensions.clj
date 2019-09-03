(ns mootz.extensions
  (:require [clojure.string :as str]
            [clojure.java.io :as io])
  (:use markdown.core))

(defn markdown [content]
  (md-to-html-string content))

(defn world [content]
  (str/replace content
               #"@world (.*?)@"
               (str "'hi " "$1" " world'")))

(defn comment [content]
  (str/replace content
               #"@commet@"
               "<form method=\"get\" action=\"/ping.mz\"><div class=\"form-group\"> <label for=\"comment\">message</label> <textarea class=\"form-control rounded-0\" name=\"comment\" id=\"comment\" rows=\"3\"></textarea> <button type=\"submit\" class=\"btn btn-dark\">save?</button></div></form>"))

(defn full-path-dir [uri]
  (let [dir (str/replace uri #"^/*|/?[^/]*\.mz" "")]
    (str "resources/public/root"
         (if (= dir "") "" "/")
         dir)))

(defn comment-read [content uri]
  (let [cfile (str (full-path-dir uri) "/comment")]
    (if (.exists (io/file cfile))
      (str/replace content
                   #".*"
                   (slurp cfile)
                   )
      content)))

(defn comment-save [uri message]
  (let [fpath (full-path-dir uri)]
    ""

    )
  )


