(ns clj-chat.db
  (:require [reagent.core :as reagent]))


;; Keeps info about authenticated user.
(defonce user (atom {}))


(defonce app-db (reagent/atom {}))


(defn cache-user [github-user]
  (when github-user
    (let [user { :username   (.-login github-user)
                 :fullname   (.-name github-user) 
                 :avatar-url (.-avatar_url github-user)
                 :bio        (.-bio github-user) }]
      (swap! app-db assoc-in [:users (:username user)] user))))


(defn get-user-info [username]
  (let [user-info (get-in @app-db [:users username])]
    (when-not user-info
      (-> (js/fetch (str "https://api.github.com/users/" username))
        (.then #(.json %))
        (.then #(cache-user %))))
    user-info))
