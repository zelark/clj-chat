(ns clj-chat.db
  (:require [reagent.core :as reagent]))


(defonce app-db (reagent/atom {:screen :sign-in}))


;; subs
(def fb-user (reagent/cursor app-db [:firebase :user]))

(def firebase-db (reagent/cursor app-db [:firebase :db]))

(def bg-url (reagent/cursor app-db [:background-url]))


(defn ls-set [k v]
  (.setItem js/localStorage (name k) v))


(defn ls-get [k]
  (.getItem js/localStorage (name k)))


(defn- cache-user [github-user]
  (when github-user
    (let [user {:username   (.-login github-user)
                :fullname   (.-name github-user) 
                :avatar-url (.-avatar_url github-user)
                :bio        (.-bio github-user)}]
      (swap! app-db assoc-in [:users (:username user)] user))))


(defn get-user-info [username]
  (let [user-info (get-in @app-db [:users username])]
    (when-not user-info
      (-> (js/fetch (str "https://api.github.com/users/" username))
        (.then #(.json %))
        (.then #(cache-user %))))
    user-info))
