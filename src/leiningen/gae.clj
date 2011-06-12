(ns leiningen.gae
  (:require [clojure.java.io :as io]
            [leiningen.help  :as lein-help]
            [leiningen.clean :as lein-clean]
            [leiningen.deps  :as lein-deps]
            [leiningen.jar   :as lein-jar]))

(defn- -x_ [s] (.replaceAll s "-" "_"))
(defn- _x- [s] (.replaceAll s "_" "-"))

(defn- strln [& xs] (apply str (interpose "\n" xs)))

(defn- mkdirs [& path] (.mkdirs (apply io/file path)))

(defn- path [& xs] (.getPath (apply io/file xs)))

(defn- write
  [content & path]
  (let [file (apply io/file path)]
    (when-not (.exists file)
      (with-open [out (io/writer file)] (.write out content)))))

(defn gae-project
  [project]
  (let [p (-x_ (:name project))
        dir-lib (path "war" "WEB-INF" "lib")]
    (assoc project
           :aot [(symbol (str p ".servlet"))]
           :library-path dir-lib
           :target-dir   dir-lib
           :extra-files-to-clean [dir-lib]
           :omit-source true)))

(def fmt-servlet
  (strln
    "(ns %s.%s"
    "  (:gen-class :extends javax.servlet.http.HttpServlet)"
    "  (:use ring.util.servlet %s.core))"
    ""
    "(defservice %s)" ))

(def fmt-core
  (strln
    "(ns %s.core)"
    ""
    "(defn %s [request]"
    "  {:status 200 :headers {} :body \"Hello World!\" })" ))

(def fmt-web_xml
  (strln
    "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
    "<!DOCTYPE web-app PUBLIC"
    " \"-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN\""
    " \"http://java.sun.com/dtd/web-app_2_3.dtd\">"
    ""
    "<web-app xmlns=\"http://java.sun.com/xml/ns/javaee\" version=\"2.5\">"
    "    <servlet>"
    "        <servlet-name>%s</servlet-name>"
    "        <servlet-class>%s.servlet</servlet-class>"
    "    </servlet>"
    "    <servlet-mapping>"
    "        <servlet-name>%s</servlet-name>"
    "        <url-pattern>/*</url-pattern>"
    "    </servlet-mapping>"
    "    <welcome-file-list>"
    "        <welcome-file>index.html</welcome-file>"
    "    </welcome-file-list>"
    "</web-app>" ))

(def fmt-appengine-web_xml
  (strln
    "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
    "<appengine-web-app xmlns=\"http://appengine.google.com/ns/1.0\">"
    "    <application>%s</application>"
    "    <version>1</version>"
    "</appengine-web-app>" ))

(defn- src-servlet
  [project]
  (let [p (:name project)]
    (format fmt-servlet p "servlet" p p p)))

(defn- src-core
  [project]
  (let [p (:name project)]
    (format fmt-core p p)))

(defn- src-web_xml
  [project]
  (let [p (:name project)]
    (format fmt-web_xml p p p)))

(defn- src-appengine-web_xml
  [project]
  (let [p (:name project)]
    (format fmt-appengine-web_xml p)))

(defn new
  "Create a new GAE project skeleton."
  [project]
  (let [p (-x_ (:name project))]
    (mkdirs "war" "WEB-INF" "lib")
    (write (src-web_xml           project) "war" "WEB-INF" "web.xml")
    (write (src-appengine-web_xml project) "war" "WEB-INF" "appengine-web.xml")
    (mkdirs "src" p)
    (write (src-core    project) "src" p "core.clj")
    (write (src-servlet project) "src" p "servlet.clj"))
  (println "Created new Google App Engine project."))

(defn deps
  "Download all dependencies and put them in war/WEB-INF/lib."
  [project]
  (lein-deps/deps project))

(defn jar
  "Build a jar file and put it in war/WEB-INF/lib."
  [project]
  (lein-jar/jar project (lein-jar/get-default-jar-name project)))

(defn clean
  "Remove compiled class files and jars from project."
  [project]
  (lein-clean/clean project)
  (mkdirs "war" "WEB-INF" "lib"))

(defn gae
  "Manage a Google App Engine application tasks."
  {:help-arglists '([new clean deps jar])
   :subtasks [#'new #'clean #'deps #'jar]}
  ([project]
   (println (lein-help/help-for "gae")))
  ([project subtask]
   ((case subtask
      "new"   new
      "deps"  deps
      "jar"   jar
      "clean" clean) (gae-project project))))
