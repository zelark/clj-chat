(ns clj-chat.fb-api
  (:require [firebase.app :as firebase-app]
            [firebase.auth :as firebase-auth]
            [firebase.database :as firebase-database]
            [firebase.storage :as firebase-storage]
            [clj-chat.db :as db]))


(def ^:const storage-url "https://firebasestorage.googleapis.com/v0/b/clj-chat.appspot.com/o/")


(defn- image-url [name]
  (str "url(" storage-url name "?alt=media)"))


(defn ls-set [k v]
  (.setItem js/localStorage (name k) v))


(defn ls-get [k]
  (.getItem js/localStorage (name k)))


(defn load-bg-url []
  (swap! db/app-db assoc :background-url (ls-get :background-url)))


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


(defn create-db []
  (RemoteDB. (.database js/firebase) "rooms/clj-group"))


(defn- get-user
  "Extract interesting user details from the Firebase auth resp."
  [firebase-user]
  (when firebase-user
    { :uid       (.-uid firebase-user)
      :fullname  (.-displayName firebase-user)
      :photo-url (.-photoURL firebase-user)
      :email     (.-email firebase-user)
      :username  (ls-get :github-username) }))


(defn- auth-changed
  [firebase-user]
  (if firebase-user
    (on-message (create-db) #(swap! db/app-db assoc :messages %))
    (off-message (create-db)))
  ;; init chat
  (->> firebase-user get-user (reset! db/fb-user)))


(defn- auth []
  (.auth js/firebase))


(defn- init-auth []
  (.onAuthStateChanged (auth) auth-changed))


(defn init-app [config]
  (.initializeApp js/firebase (clj->js config))
  (init-auth))


(defn sign-in-with-github []
  (-> (.signInWithPopup (auth) (new js/firebase.auth.GithubAuthProvider))
      (.then #(->> (.. % -additionalUserInfo -username)
                   (ls-set :github-username)))))


(defn sign-out []
  (.signOut (auth)))


(defn upload-file [file]
  (-> (.storage js/firebase)
      (.ref)
      (.child (str "u/" (:uid @db/fb-user) "/imgs/" (.-name file)))
      (.put file)
      (.then #(->> (.. % -metadata -fullPath)
                   js/encodeURIComponent
                   image-url
                   (ls-set :background-url)))
      (.then #(load-bg-url))
      (.catch #(console.log %))))