(ns clj-chat.routing
  (:require [clj-chat.db :as db]
            [bidi.bidi :as bidi]
            [accountant.core :as accountant]))


(def app-routes
  ["/" {""                     :sign-in
        "chat"                 :chat
        ["profile/" :username] :profile
        true                   :nothing}])


(defn path-for [handler]
  (bidi/path-for app-routes handler))


(defn init! []
  (accountant/configure-navigation!
    {:nav-handler (fn [path]
                    (let [match (bidi/match-route app-routes path)
                          screen (:handler match)
                          params (:route-params match)]
                      (swap! db/app-db assoc :route {:screen screen :params params})))
    :path-exists? (fn [path]
                    (boolean (bidi/match-route app-routes path)))})
  (accountant/dispatch-current!))