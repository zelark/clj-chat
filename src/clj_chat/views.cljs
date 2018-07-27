(ns clj-chat.views
  (:require [clj-chat.db :as db]
            [clj-chat.fb-api :as fb]
            [reagent.core :as reagent]
            [secretary.core :as secretary]
            [clj-chat.routing :as routing]))


(defn- at [s] (str "@" s))


(defn- ^boolean me? [username]
  (= username (:username @db/fb-user)))


(defn- local-date [date-str]
  (.toLocaleString (js/Date. date-str)))


(defn- scroll-to-bottom [this]
  (let [node (reagent/dom-node this)
        opts #js {:top (.-scrollHeight node)
                  :behavior "smooth"}]
    (.scrollBy node opts)))


;; Components
(defn sign-in []
  [:div.screen.sign-in
   [:button.button {:on-click #(fb/sign-in-with-github)} "Sign In with GitHub"]])


(defn message [{:keys [user uid time body]}]
  [:div {:class (str "message " (if (me? user) "me" "other"))}
   [:img.avatar {:src (:avatar-url (db/get-user-info user))
                 :on-click #(routing/navigate! (str "/profile/" user))}]
   [:div.message-buble
    [:div.message-meta
     [:div.message-user (at user)]
     [:div.message-time (local-date time)]]
    [:div.message-text body]]])


(defn messages-list []
  (reagent/create-class
    {:display-name "messages-list"
     :component-did-mount scroll-to-bottom
     :component-did-update scroll-to-bottom
     :reagent-render (fn []
                       [:div.content
                        (when (some? @db/bg-url)
                          {:style {:background-image @db/bg-url}})
                        (doall
                          (for [[key msg] (sort (:messages @db/app-db))]
                            ^{:key key} [message msg]))])}))


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
            [:img.avatar {:src photo-url}]]]]
         [messages-list]
         [:div.footer
          [:textarea.input {:value @input
                            :on-change #(reset! input (-> % .-target .-value))}]
          [:button.button
           {:on-click #(when (seq @input)
                         (fb/post-message @db/firebase-db
                                          {:user username
                                           :uid  uid
                                           :time (.toUTCString (js/Date.))
                                           :body (clojure.string/trim @input)})
                         (reset! input ""))}
           "Send"]]]))))


(defn profile []
  (let [gh-user (db/get-user-info (:selected-user @db/app-db))
        {:keys [username fullname avatar-url bio]} gh-user]
    [:div.screen.profile
     [:div.header
      [:div.header-left
       [:small
        [:a {:href "#/chat"} "back to chat"]]]
      [:div.header-title (at username)]
      [:div.header-right]]
     [:div.content
      (when (some? @db/bg-url)
        { :style { :background-image @db/bg-url }})
      [:img.avatar.avatar-xl {:src avatar-url}]
      [:div.profile-info
       [:div.username (at username)]
       [:div.full-name fullname]
       [:div.bio bio]]
      (when (me? username)
        [:div
         [:input#background-image {:type "file" 
                                   :accept "image/png, image/jpeg" 
                                   :on-change #(let [target (.-currentTarget %)
                                                     file   (-> target .-files (aget 0))]
                                                 (if (> (.-size file) 5242880)
                                                   (js/alert "File size shouldn't be greater than 5 MB!")
                                                   (fb/upload-file file (:uid @db/fb-user)))
                                                 (set! (.-value target) ""))}]

         [:button.button
          {:on-click #(-> (.getElementById js/document "background-image") .click)}
          "Upload background"]
         [:button.button {:on-click #(fb/sign-out)} "sign out"]])]]))


(defn app []
  (case (:screen @db/app-db)
    :sign-in [sign-in]
    :chat    [chat]
    :profile [profile]))
