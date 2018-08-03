(ns clj-chat.firebase
  (:require [firebase.app :as firebase-app]
            [firebase.auth :as firebase-auth]
            [firebase.storage :as firebase-storage]
            [firebase.database :as firebase-database]))


(defn- auth [] (.auth js/firebase))
(defn- db [] (.database js/firebase))
(defn- db-ref [path] (.ref (db) path))


(defn- github-auth-provider []
  (new (.. js/firebase -auth -GithubAuthProvider)))


(defn- init-auth [callback]
  (.onAuthStateChanged (auth) callback))


(defn init-app [config auth-changed-callback]
  (.initializeApp js/firebase (clj->js config))
  (init-auth auth-changed-callback))


(defn sign-in-with-popup [provider]
  (-> (auth)
      (.signInWithPopup provider)))


(defn sign-in-with-github [{:keys [callback]}]
  (-> (sign-in-with-popup (github-auth-provider))
      (.then #(callback (.. % -additionalUserInfo -username)))))


(defn sign-out [_] (.signOut (auth)))


(defn push [{:keys [chatname message]}]
  (-> (db-ref (str "rooms/" chatname))
      (.push)
      (.set (clj->js message))))


(defn on-value [path callback]
  (let [ref (db-ref (str "rooms/" path))
        val (fn [res] (-> res .val (js->clj :keywordize-keys true)))]
    (.on ref "value" #(-> % val callback))))


(defn upload-file [{:keys [file uid callback]}]
  (-> (.storage js/firebase)
      (.ref)
      (.child (str "u/" uid "/imgs/" (.-name file)))
      (.put file)
      (.then #(callback (.. % -metadata -fullPath)))
      (.catch #(console.log %))))
