(ns clj-chat.core
  (:require [reagent.core :as reagent]
            [clj-chat.views :as views]
            [devtools.core :as devtools]
            [clj-chat.fb-api :as fb]))


(devtools/install!)
(enable-console-print!)


;; ENTRY POINT
(defn ^:export run []         ;; Why figwheel does not call it?
  (let [firebase-app-config { :apiKey            "AIzaSyCEhmVGg3qnpqSnPwAFpCHdRqwsR5abkhU"
                              :authDomain        "clj-chat.firebaseapp.com"
                              :databaseURL       "https://clj-chat.firebaseio.com"
                              :projectId         "clj-chat"
                              :storageBucket     "clj-chat.appspot.com"
                              :messagingSenderId "293369282958" }]
    (fb/load-bg-url)
    (println "init app...")
    (fb/init-app firebase-app-config))
    #_(reagent/render-component [views/app]
                              (.getElementById js/document "app")))

(reagent/render-component [views/app] ;; FIXME
                              (.getElementById js/document "app"))

