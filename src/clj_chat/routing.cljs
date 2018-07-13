(ns clj-chat.routing
  (:require-macros [secretary.core :refer [defroute]])
  (:require [clj-chat.db :as db]
            [goog.events :as events]
            [secretary.core :as secretary])
  (:import [goog History]
           [goog.history EventType Html5History]))



(defroute "/" []
  (swap! db/app-db assoc :screen :sign-in))

(defroute "/profile/:username" [username]
  (swap! db/app-db assoc :screen :profile :selected-user username))
  
(defroute "/:state" [state]
  (swap! db/app-db assoc :screen (keyword state)))


(def history
  (doto (History.)
    (events/listen EventType.NAVIGATE
                   #(secretary/dispatch! (.-token %)))
    (.setEnabled true)))


(defn navigate! [token]
   (.setToken history token))