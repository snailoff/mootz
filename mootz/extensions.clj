(ns mootz.extensions
  (:require [mootz.private :as prv]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:use markdown.core))


(defn markdown [content]
  (md-to-html-string content))


(defn images [content uri]
  (let [dir (if (prv/is-dir? uri)
              uri
              (string/replace uri #"/?[^/]*$" ""))]
    (string/replace content
                    #"@images@"
                    (string/join ""
                                 (map #(str "<p><img src=\""
                                            %1
                                            "\" class=\"img-fluid\""
                                            " /></p>")
                                      (filter #(and
                                                (string/starts-with? % uri)
                                                (some? (re-matches #"^.*\.(jpg|png|gif)$" (str %))))
                                              (map #(string/replace %1 #"^resources/public/root/" "/")
                                                   (.listFiles (io/file (prv/full-path dir))))))))))

;(defn comment [content]
;  (string/replace content
;               #"@commet@"
;               "<form method=\"get\" action=\"/ping.mz\"><div class=\"form-group\"> <label for=\"comment\">message</label> <textarea class=\"form-control rounded-0\" name=\"comment\" id=\"comment\" rows=\"3\"></textarea> <button type=\"submit\" class=\"btn btn-dark\">save?</button></div></form>"))
;
;(defn full-path-dir [uri]
;  (let [dir (string/replace uri #"^/*|/?[^/]*\.mz" "")]
;    (str "resources/public/root"
;         (if (= dir "") "" "/")
;         dir)))
;
;(defn comment-read [content uri]
;  (let [cfile (str (full-path-dir uri) "/comment")]
;    (if (.exists (io/file cfile))
;      (string/replace content
;                   #".*"
;                   (slurp cfile)
;                   )
;      content)))
;
;(defn comment-save [uri message]
;  (let [fpath (full-path-dir uri)]
;    ""
;
;    )
;  )



