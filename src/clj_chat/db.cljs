(ns clj-chat.db
  (:require [re-frame.core :as re-frame]))


(defonce default-db
  { :firebase { :app-config { :apiKey            "AIzaSyCEhmVGg3qnpqSnPwAFpCHdRqwsR5abkhU"
                              :authDomain        "clj-chat.firebaseapp.com"
                              :databaseURL       "https://clj-chat.firebaseio.com"
                              :projectId         "clj-chat"
                              :storageBucket     "clj-chat.appspot.com"
                              :messagingSenderId "293369282958" }
                :storage-url "https://firebasestorage.googleapis.com/v0/b/clj-chat.appspot.com/o/"}})


(defn ls-get [k]
  (.getItem js/localStorage (name k)))


(re-frame/reg-cofx
  ::localstore
  (fn [cofx _]
    (assoc cofx
      :username       (ls-get :github-username)
      :background-url (ls-get :background-url))))
