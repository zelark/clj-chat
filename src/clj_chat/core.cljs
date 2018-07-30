(ns clj-chat.core
  (:require [clj-chat.fb-api :as fb]
            [reagent.core :as reagent]
            [clj-chat.views :as views]
            [clj-chat.routing]))


(enable-console-print!)


(defn on-js-reload []
  (reagent/render-component [views/app]
                            (.getElementById js/document "app")))


;; ENTRY POINT
(defn ^:export run []
  (let [firebase-app-config #js {:apiKey            "AIzaSyCEhmVGg3qnpqSnPwAFpCHdRqwsR5abkhU"
                                 :authDomain        "clj-chat.firebaseapp.com"
                                 :databaseURL       "https://clj-chat.firebaseio.com"
                                 :projectId         "clj-chat"
                                 :storageBucket     "clj-chat.appspot.com"
                                 :messagingSenderId "293369282958"}]
    (println "init app...")
    (fb/init-app firebase-app-config))
    (clj-chat.routing/init!)
    (on-js-reload))
