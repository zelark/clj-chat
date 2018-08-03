(ns clj-chat.core
  (:require [clj-chat.routing]
            [clj-chat.subs :as subs]
            [reagent.core :as reagent]
            [clj-chat.views :as views]
            [clj-chat.events :as evns]
            [clj-chat.firebase :as firebase]))


(enable-console-print!)


;; Every time when auth state changes, this fn is called.
;; See firebase/init-app below.
(defn auth-changed [firebase-user]
  (when (some? firebase-user)
    (let [user {:uid       (.-uid firebase-user)
                :fullname  (.-displayName firebase-user)
                :photo-url (.-photoURL firebase-user)
                :email     (.-email firebase-user)}]
    (evns/fire [::evns/set-user user]))))


(defn on-js-reload []
  (reagent/render-component [views/app]
                            (.getElementById js/document "app")))


;; ENTRY POINT
(defn ^:export run []
  ;; First of all, we are initializing app-db by db/default-db
  ;; which contains firebase app's settings.
  (evns/fire-sync [::evns/initialize-db])
  (let [app-config (subs/<sub [::subs/app-config])]
    (println "init app...")
    (firebase/init-app app-config auth-changed))
  (clj-chat.routing/init!)
  (on-js-reload))
