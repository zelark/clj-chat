(ns clj-chat.routing
  (:require [bidi.bidi :as bidi]
            [clj-chat.events :as evns]
            [accountant.core :as accountant]))


(def routes
  ["/" {""                     :sign-in
        ["chat/" :chatname]    :chat
        ["profile/" :username] :profile
        true                   :nothing}])


(defn path-for [& args]
  (apply bidi/path-for routes args))


(defn init! []
  (accountant/configure-navigation!
    {:nav-handler (fn [path]
                    (let [match (bidi/match-route routes path)
                          route {:screen (:handler match)
                                 :params (:route-params match)}]
                      (evns/fire [::evns/set-route route])))
    :path-exists? (fn [path]
                    (boolean (bidi/match-route routes path)))})
  (accountant/dispatch-current!))