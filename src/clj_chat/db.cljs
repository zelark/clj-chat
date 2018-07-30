(ns clj-chat.db
  (:require [reagent.core :as reagent]
            [goog.object :as gobj]))


(defonce app-db (reagent/atom {}))


;; subs
(def fb-user (reagent/cursor app-db [:firebase :user]))

(def firebase-db (reagent/cursor app-db [:firebase :db]))

(def bg-url (reagent/cursor app-db [:background-url]))

(def route (reagent/cursor app-db [:route]))


(defn ls-set [k v]
  (.setItem js/localStorage (name k) v))


(defn ls-get [k]
  (.getItem js/localStorage (name k)))


(defn- cache-user [github-user]
  (when github-user
    (let [user {:username   (gobj/get github-user "login")
                :fullname   (gobj/get github-user "name")
                :avatar-url (gobj/get github-user "avatar_url")
                :bio        (gobj/get github-user "bio")}]
      (swap! app-db assoc-in [:users (:username user)] user))))


(defn get-user-info [username]
  (let [user-info (get-in @app-db [:users username])]
    (when-not user-info
      (-> (js/fetch (str "https://api.github.com/users/" username))
        (.then #(.json %))
        (.then #(cache-user %))))
    user-info))
