(ns mssngvwls.ring.middleware.safe-jar-resource
  (:require [ring.middleware.head :as head]
            [ring.util.codec :as codec]
            [ring.util.request :as request]
            [ring.util.response :as response]
            [ring.util.time :refer (format-date)]
            [clojure.java.io :as io]))

(defn- connection-content-length [^java.net.URLConnection conn]
  (let [len (.getContentLength conn)]
    (if (<= 0 len) len)))

(defn- add-ending-slash [^String path]
  (if (.endsWith path "/")
    path
    (str path "/")))

(defn- jar-directory? [^java.net.JarURLConnection conn]
  (let [jar-file   (.getJarFile conn)
        entry-name (.getEntryName conn)
        dir-entry  (.getEntry jar-file (add-ending-slash entry-name))]
    (and dir-entry (.isDirectory dir-entry))))

(defn safe-resource-data
  "File-descriptor-leak safe version of the jar resource-data
   function."
  [^java.net.URL url]
  (let [conn (.openConnection url)]
    (if-not (jar-directory? conn)
      {:content (.getInputStream conn)
       :content-length (connection-content-length conn)
       ;; and here's the replacement of the problematic line. Ring's
       ;; version of this (which correctly reads the modification date
       ;; on the in-jar file) was opening a file handle every on the
       ;; jar every time. This might properly be considered a Java
       ;; bug.
       :last-modified (java.util.Date.)})))

(defn safe-url-response
  [^java.net.URL url]
  (if-let [data (safe-resource-data url)]
    (cond-> (response/response (:content data))
      (:content-length data) (response/header "Content-Length" (:content-length data))
      (:last-modified data) (response/header "Last-Modified" (format-date (:last-modified data))))))

(defn safe-resource-response
  "File-descriptor-leak safe version of resource-response."
  [path & [{:keys [root loader] :as opts}]]
  (let [path (-> (str (or root "") "/" path)
                 (.replace "//" "/")
                 (.replaceAll "^/" ""))]
    (when-let [resource (if loader
                          (io/resource path loader)
                          (io/resource path))]
      (if (= "jar" (.getProtocol resource))
        (safe-url-response resource)
        (response/resource-response path opts)))))

(defn safe-resource-request
  "File-descriptor-leak safe version of resource-request."
  [request root-path & [{:keys [loader]}]]
  (when (#{:head :get} (:request-method request))
    (let [path (subs (codec/url-decode (request/path-info request)) 1)]
      (-> (safe-resource-response path {:root root-path :loader loader})
          (head/head-response request)))))

(defn wrap-safe-jar-resource
  "A replacement for wrap-resource that avoids the problem with
   leaking file descriptors for in-jar resources that `wrap-resource`
   has."
  [handler root-path & [{:keys [loader]}]]
  (fn [request]
    (or (safe-resource-request request root-path {:loader loader})
        (handler request))))
