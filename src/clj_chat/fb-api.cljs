(ns clj-chat.fb-api
  (:require [clj-chat.db :as db]
            [accountant.core :as accountant]
            [firebase.app :as firebase-app]
            [firebase.auth :as firebase-auth]
            [firebase.database :as firebase-database]
            [firebase.storage :as firebase-storage]))


(def ^:const storage-url "https://firebasestorage.googleapis.com/v0/b/clj-chat.appspot.com/o/")


(defn- image-url [name]
  (str "url(" storage-url (js/encodeURIComponent name) "?alt=media)"))


(defn- load-bg-url []
  (reset! db/bg-url (db/ls-get :background-url)))


(defprotocol IFirebaseDB

  (into-room [this])

  (post-message [this message])

  (on-message [this f])

  (off-message [this]))


(deftype RemoteDB [db room-id]
  IFirebaseDB

  (into-room [this]
    (.ref db room-id))

  (post-message [this message]
    (-> (into-room this)
        .push
        (.set (clj->js message))))

  (on-message [this callback]
    (-> (into-room this)
        (.on "value" #(-> % .val (js->clj :keywordize-keys true) callback))))

  (off-message [this]
    (-> (into-room this)
        .off)))


(defn- create-db []
  (RemoteDB. (.database js/firebase) "rooms/clj-group"))


(defn- get-user
  "Extract interesting user details from the Firebase auth resp."
  [firebase-user]
  (when firebase-user
    { :uid       (.-uid firebase-user)
      :fullname  (.-displayName firebase-user)
      :photo-url (.-photoURL firebase-user)
      :email     (.-email firebase-user)
      :username  (db/ls-get :github-username) }))


(defn- auth-changed
  [firebase-user]
  (if (some? firebase-user)
    (do
      ;; init chat
      (->> firebase-user get-user (reset! db/fb-user))
      (reset! db/firebase-db (create-db))
      (load-bg-url)
      (on-message @db/firebase-db #(swap! db/app-db assoc :messages %))
    )
    (do
      (when (some? @db/firebase-db)
        (off-message @db/firebase-db))
      (accountant/navigate! "/")
    )))


(defn- auth []
  (.auth js/firebase))


(defn- init-auth []
  (.onAuthStateChanged (auth) auth-changed))


(defn init-app [config]
  (.initializeApp js/firebase config)
  (init-auth))


(defn sign-in-with-github []
  (-> (.signInWithPopup (auth) (new js/firebase.auth.GithubAuthProvider))
      (.then #(->> (.. % -additionalUserInfo -username)
                   (db/ls-set :github-username)))
      (.then (accountant/navigate! "/chat"))))


(defn sign-out []
  (.signOut (auth)))


(defn upload-file [file uid]
  (-> (.storage js/firebase)
      (.ref)
      (.child (str "u/" uid "/imgs/" (.-name file)))
      (.put file)
      (.then #(->> (.. % -metadata -fullPath)
                   image-url
                   (db/ls-set :background-url)))
      (.then #(load-bg-url))
      (.catch #(console.log %))))
