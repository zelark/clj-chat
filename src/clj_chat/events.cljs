(ns clj-chat.events
  (:require [clj-chat.db :as db]
            [clojure.string :as string]
            [re-frame.core :as re-frame]
            [clj-chat.firebase :as firebase]
            [accountant.core :as accountant]))


(def fire re-frame/dispatch)
(def fire-sync re-frame/dispatch-sync)


;; Events
(re-frame/reg-event-fx
  ::initialize-db
  [(re-frame/inject-cofx ::db/localstore)]
  (fn [{:keys [db username background-url]} _]
    (let [localstore {:firebase       {:user {:username username}}
                      :background-url background-url}]
      {:db (merge-with merge db/default-db localstore)})))


(re-frame/reg-event-fx
  ::sign-in
  (fn [_ _]
    {::firebase/sign-in {:callback #(fire [::set-username %])}
     ::navigate "/chat/clj-group"}))


(re-frame/reg-event-fx
  ::sign-out
  (fn [_ _]
    {::firebase/sign-out {}
     ::navigate "/"}))


(re-frame/reg-event-fx
  ::send-message
  [(re-frame/inject-cofx ::time)]
  (fn [{:keys [time]} [_ chatname username uid text]]
    {::firebase/push {:chatname chatname
                      :message {:user username
                                :uid  uid
                                :time time
                                :body (string/trim text)}}}))


(re-frame/reg-event-fx
  ::upload-file
  (fn [_ [_ file uid]]
    {::firebase/upload-file {:file     file
                             :uid      uid 
                             :callback #(fire [::set-background-url %])}}))


(re-frame/reg-event-db
  ::set-route
  (fn [db [_ route]]
    (assoc db :route route)))


(re-frame/reg-event-fx
  ::set-username
  (fn [{:keys [db]} [_ username]]
    {:db    (assoc-in db [:firebase :user :username] username)
     ::save [:github-username username]}))


(re-frame/reg-event-fx
  ::set-background-url
  (fn [{:keys [db]} [_ url]]
    {:db    (assoc db :background-url url)
     ::save [:background-url url]}))


(re-frame/reg-event-db
  ::set-user
  (fn [db [_ user]]
    (update-in db [:firebase :user] merge user)))


(re-frame/reg-event-db
  ::write-to
  (fn [db [_ path value]]
    (assoc-in db path value)))


;; Effects
(re-frame/reg-fx ::navigate accountant/navigate!)


(re-frame/reg-fx
  ::save
  (fn [[key value]]
    (.setItem js/localStorage (name key) value)))


;; Firebase Effects
(re-frame/reg-fx ::firebase/sign-in firebase/sign-in-with-github)
(re-frame/reg-fx ::firebase/sign-out firebase/sign-out)
(re-frame/reg-fx ::firebase/push firebase/push)
(re-frame/reg-fx ::firebase/upload-file firebase/upload-file)


;; Co-effects
(re-frame/reg-cofx
  ::time
  (fn [cofx _]
    (assoc cofx :time (.toUTCString (js/Date.)))))
