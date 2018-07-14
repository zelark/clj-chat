(ns clj-chat.views
  (:require [clj-chat.fb-api :as fb]
            [secretary.core :as secretary]
            [clj-chat.routing :as routing]
            [reagent.core :as reagent]
            [clj-chat.db :as db]))


(defn- at [s] (str "@" s))


(defn- me? [username]
  (= username (:username @db/fb-user)))


(defn- local-date [date-str]
  (.toLocaleString (js/Date. date-str)))


;; Components
(defn sign-in []
  [:div.screen.sign-in
   [:button.button
    { :on-click (fn [e]
                  (fb/sign-in-with-github)
                  (routing/navigate! "/chat")) }
    "Sign In with GitHub"]])


(defn message
  [{:keys [user uid time body]}]
  [:div {:class (str "message " (if (me? user) "me" "other"))}
   [:img.avatar { :src      (:avatar-url (db/get-user-info user))
                  :on-click #(routing/navigate! (str "/profile/" user)) }]
   [:div.message-buble
    [:div.message-meta
     [:div.message-user (at user)]
     [:div.message-time (local-date time)]]
    [:div.message-text body]]])


(defn chat []
  (let [input (reagent/atom "")]
    (fn []
      (let [{:keys [username photo-url uid]} @db/fb-user]
        [:div.screen
         [:div.header
          [:div.header-left]
          [:div.header-title "Clojure Learning Group"]
          [:div.header-right
           [:a {:href (str "#/profile/" username)}
            [:img.avatar { :src photo-url }]]]]

         [:div.content
          (when (:background-url @db/app-db)
            {:style {:background-image (:background-url @db/app-db)}})
          (doall
            (for [[key msg] (:messages @db/app-db)]
              ^{:key key} [message msg]))]
  
         [:div.footer
          [:textarea.input { :value     @input
                             :on-change #(reset! input (-> % .-target .-value)) }]
          [:button.button {:on-click #(when (seq @input)
                                        (fb/post-message (fb/create-db)
                                                         { :user username
                                                           :uid  uid
                                                           :time (.toUTCString (js/Date.))
                                                           :body (clojure.string/trim @input) })
                                        (reset! input ""))}
                          "Send"]]]))))


(defn profile []
  (let [user (db/get-user-info (:selected-user @db/app-db))
        {:keys [username fullname avatar-url bio]} user]
    [:div.screen.profile
     [:div.header
      [:div.header-left
       [:small
        [:a {:href "#/chat"} "back to chat"]]]
      [:div.header-title (at username)]
      [:div.header-right]]
     [:div.content
      (when (:background-url @db/app-db)
        {:style {:background-image (:background-url @db/app-db)}})
      [:img.avatar.avatar-xl {:src avatar-url}]
      [:div.profile-info
       [:div.username (at username)]
       [:div.full-name fullname]
       [:div.bio bio]]
      (when (me? username)
        [:div
         [:input#background-image { :type "file" 
                                    :accept "image/png, image/jpeg" 
                                    :on-change (fn [e]
                                                 (let [target (.-currentTarget e)
                                                       file (-> target .-files (aget 0))
                                                       size (.-size file)]
                                                   (if (> size 5242880)
                                                     (js/alert "File size shouldn't be greater than 5 MB!")
                                                     (fb/upload-file file))
                                                   (set! (.-value target) ""))) }]

         [:button.button { :on-click #(-> (.getElementById js/document "background-image")
                                          .click) }
                         "Upload background"]
         [:button.button
          {:on-click (fn [e]
                       (fb/sign-out)
                       (routing/navigate! "/sign-in"))}
          "sign out"]])]]))


(defn app []
  (case (:screen @db/app-db)
    :sign-in [sign-in]
    :chat    [chat]
    :profile [profile]))
