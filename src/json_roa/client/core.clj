; Copyright (C) 2015-2017 Dr. Thomas Schank  (DrTom@schank.ch, Thomas.Schank@algocon.ch)

(ns json-roa.client.core
  (:refer-clojure :exclude [get])
  (:require
    [clj-http.client :as http-client]
    [uritemplate-clj.core :refer [uritemplate]]
    [clojurewerkz.urly.core :as urly]
    [logbug.debug :as debug :refer [identity-with-logging]]
    [logbug.ring :as logbug-ring]
    [logbug.thrown :as thrown]
    [logbug.catcher :as catcher]
    [clojure.tools.logging :as logging]
    )
  (:import
    [java.net URI URL]
    ))

;##############################################################################

(defn get-root [url & {:keys [default-conn-opts default-middleware]
                       :or {default-conn-opts {}
                            default-middleware http-client/default-middleware}}]
  (let [conn-opts (merge {:method :get
                          :url url
                          :accept "application/json-roa+json"
                          :as :json}
                         default-conn-opts)]
    (assoc (http-client/with-middleware default-middleware
             (http-client/request conn-opts))
           :roa-conn-opts (assoc conn-opts :middleware default-middleware))))

(defn data [response]
  (when response
    (-> response :body (dissoc :_json-roa))))

(defn json-roa-data [response]
  (-> response :body :_json-roa))

(defn relation [response rel-key]
  (catcher/with-logging {}
    (-> response
        json-roa-data
        :relations
        rel-key
        (assoc :roa-conn-opts (:roa-conn-opts response)))))

(defn build-uri [base-uri relation-uri url-params]
  (urly/resolve
    base-uri
    (uritemplate relation-uri
                 (clojure.walk/stringify-keys url-params))))

(defn get
  "DEPRECATED use request."
  [relation url-params & {:keys [mod-conn-opts]
                                  :or {mod-conn-opts identity}}]
  (catcher/with-logging {}
    (when relation
      (let [conn-opts (-> relation :roa-conn-opts mod-conn-opts)
            uri (build-uri (-> conn-opts :url)
                           (-> relation :href)
                           url-params)
            middleware (-> conn-opts :middleware)]
        (assoc
          (http-client/with-middleware middleware
            (http-client/request
              (merge
                (dissoc conn-opts :roa-conn-opts)
                {:url uri})))
          :roa-conn-opts conn-opts)))))

(defn request
  "Submits a HTTP requests. The first and mandatory argument is the roa-relation.
  The second optional parameter is a map specifying the url-parameters used for
  templating the URL and defaults to the empty map. The third argument is the method
  and defaults to :get. The fourth argument is a map of parameters passed to http-client.
  The fifths argument is a function which gets passed the parameters for http-client. The
  return value will be the parameters passed to http-client."
  [rel & [url-params method opts mod-opts]]
  (let [url-params (or url-params {})
        method (or method :get)
        opts (or opts {})
        mod-opts (or mod-opts identity)]
    (let [roa-conn-opts (-> rel :roa-conn-opts)
          uri (build-uri (-> roa-conn-opts :url)
                         (-> rel :href)
                         url-params)
          params (-> roa-conn-opts
                     (merge {:url uri
                             :method method})
                     (merge opts)
                     mod-opts)
          middleware (-> roa-conn-opts :middleware)]
      (assoc
        (http-client/with-middleware middleware
          (http-client/request params))
        :roa-conn-opts roa-conn-opts))))


;##############################################################################

(declare coll-seq)

(defn- coll-next [next-rel rels]
  (lazy-seq
    (if-let [[fst-rel] (seq rels)]
      (cons fst-rel (coll-next next-rel (rest rels)))
      (when (:href next-rel)
        (coll-seq (get next-rel {}))))))

(defn coll-seq [response]
  (let [conn-opts (-> response :roa-conn-opts)
        middleware (-> conn-opts :middleware)]
    (when-let [collection (-> response json-roa-data :collection)]
      (logging/debug 'COLLECTION collection)
      (let [rels (->> collection :relations
                      (into [])
                      (sort-by first)
                      (map second)
                      (map #(assoc % :roa-conn-opts conn-opts)))
            next-rel (-> collection :next (or {})
                         (assoc :roa-conn-opts conn-opts))]
        (logging/debug 'NEXT-REL next-rel)
        (when (seq rels) (coll-next next-rel rels))))))


;##############################################################################

;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)
