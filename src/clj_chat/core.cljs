(ns clj-chat.core
  (:require [clj-chat.fb-api :as fb]
            [reagent.core :as reagent]
            [clj-chat.views :as views]
            [devtools.core :as devtools]))


(enable-console-print!)


;; ENTRY POINT
(defn ^:export run [] ;; Why figwheel does not call it when I change the code?
  (let [firebase-app-config #js {:apiKey            "AIzaSyCEhmVGg3qnpqSnPwAFpCHdRqwsR5abkhU"
                                 :authDomain        "clj-chat.firebaseapp.com"
                                 :databaseURL       "https://clj-chat.firebaseio.com"
                                 :projectId         "clj-chat"
                                 :storageBucket     "clj-chat.appspot.com"
                                 :messagingSenderId "293369282958"}]
    (println "init app...")
    (fb/init-app firebase-app-config))
    (reagent/render-component [views/app]
                              (.getElementById js/document "app")))


