(ns clj-chat.subs
  (:require [goog.object :as gobj]
            [reagent.ratom :as ratom]
            [clj-chat.events :as evns]
            [re-frame.core :as re-frame]
            [clj-chat.firebase :as firebase]))


(def <sub (comp deref re-frame/subscribe))


(re-frame/reg-sub
  ::route
  (fn [db _]
    (:route db)))


(re-frame/reg-sub
  ::firebase-data
  (fn [db _]
    (:firebase db)))


(re-frame/reg-sub
  ::app-config
  :<-[::firebase-data]
  #(:app-config %))


(re-frame/reg-sub
  ::storage-url
  :<-[::firebase-data]
  #(:storage-url %))


(re-frame/reg-sub
  ::user
  :<- [::firebase-data]
  #(:user %))


(re-frame/reg-sub
  ::bg-url
  (fn [db _]
    (let [storage-url    (<sub [::storage-url])
          background-url (:background-url db)
          encoded-url    (js/encodeURIComponent background-url)]
      (str "url(" storage-url encoded-url "?alt=media)"))))


(re-frame/reg-sub
  ::scroll-top
  (fn [db _]
    (:scroll-top db)))


(defn- github-user->userinfo [github-user]
  (when (some? github-user)
    {:username   (gobj/get github-user "login")
     :fullname   (gobj/get github-user "name")
     :avatar-url (gobj/get github-user "avatar_url")
     :bio        (gobj/get github-user "bio")}))


(re-frame/reg-sub-raw
  ::userinfo
  (fn [app-db [_ username]]
    (let [callback #(re-frame/dispatch [:clj-chat.events/write-to 
                                        [:cache :users username]
                                        (github-user->userinfo %)])]
      (-> (js/fetch (str "https://api.github.com/users/" username))
        (.then #(.json %))
        (.then callback))
      (ratom/reaction (get-in @app-db [:cache :users username])))))


(re-frame/reg-sub-raw
  ::on-value
  (fn [app-db [_ chatname]]
    (let [path     [:cache :chats chatname]
          callback #(evns/fire [::evns/write-to path %])]
      (firebase/on-value chatname callback)
      (ratom/reaction (get-in @app-db path)))))


(re-frame/reg-sub
  ::sorted-messages
  (fn [[_ chatname] _]
    (re-frame/subscribe [::on-value chatname]))
  #(sort %))

