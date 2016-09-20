(defproject mssngvwls/ring-safe-jar-resource "0.1.0"
  :description "A version of ring's wrap-resources middleware that fixes the in-jar file handle leak problem."
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [ring/ring-core "1.5.0"]])