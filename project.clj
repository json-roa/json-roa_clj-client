(defproject json-roa_clj-client "0.1.0"
  :description "Clojure JSON-ROA Client Reference Implementation"
  :url "https://github.com/json-roa/json-roa_clj-client"
  :license {:name "Dual: EPL and LGPL"}
  :dependencies [
                 [cheshire "5.5.0"]
                 [clj-http "2.0.0"]
                 [clojurewerkz/urly "1.0.0"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail javax.jms/jms com.sun.jdmk/jmxtools com.sun.jmx/jmxri]]
                 [logbug "2.0.0-beta.8"]
                 [org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.cli "0.3.3"]
                 [org.slf4j/slf4j-log4j12 "1.7.12"]
                 [uritemplate-clj "1.1.1"]
                 ])
