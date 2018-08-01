(ns clj-chat.views
  (:require [clj-chat.db :as db]
            [clj-chat.fb-api :as fb]
            [reagent.core :as reagent]
            [clj-chat.routing :as routing]
            [accountant.core :as accountant]))


(defn- at [s] (str "@" s))


(defn- ^boolean me? [username]
  (= username (:username @db/fb-user)))


(defn- local-date [date-str]
  (.toLocaleString (js/Date. date-str)))


(defn- scroll-to-bottom [behavior this]
  (let [node (reagent/dom-node this)
        scroll-top (or (:scroll-top @db/app-db)
                       (.-scrollHeight node))
        opts #js {:top scroll-top :behavior behavior}]
    (.scrollBy node opts)))


(defn- keep-scroll-position [this]
  (let [node (reagent/dom-node this)
        scroll-top (.-scrollTop node)]
    (swap! db/app-db assoc :scroll-top scroll-top)))


;; Components
(defn header [{:keys [left title right]}]
  [:div.header
   [:div.header-left left]
   [:div.header-title title]
   [:div.header-right right]])


(defn avatar [{:keys [src username size]}]
  (let [on-click-fn (when (some? username)
                      #(accountant/navigate! (str "/profile/" username)))]
    [:img.avatar
     {:src src
      :class (when (some? size) (name size))
      :on-click on-click-fn}]))


(defn message [{:keys [user uid time body]}]
  [:div.message {:class (if (me? user) "me" "other")}
   [avatar {:src (:avatar-url (db/get-user-info user))
            :username user}]
   [:div.message-buble
    [:div.message-meta
     [:div.message-user (at user)]
     [:div.message-time (local-date time)]]
    [:div.message-text body]]])


(defn messages-list []
  (reagent/create-class
    {:display-name "messages-list"
     :component-did-mount (partial scroll-to-bottom "instant")
     :component-did-update (partial scroll-to-bottom "smooth")
     :component-will-unmount keep-scroll-position
     :reagent-render (fn []
                       [:div.content
                        (when (some? @db/bg-url)
                          {:style {:background-image @db/bg-url}})
                        (doall
                          (for [[key msg] (sort (:messages @db/app-db))]
                            ^{:key key} [message msg]))])}))


(defn back-to-chat []
  [:small [:a {:href (routing/path-for :chat :chatname "clj-chat")} "back to chat"]]) ;; FIXME


;; Main components
(defn sign-in []
  [:div.screen.sign-in
   [:button.button {:on-click #(fb/sign-in-with-github)} "Sign In with GitHub"]])


(defn chat [_]
  (let [input (reagent/atom "")]
    (fn [{:keys [chatname]}]
      (let [{:keys [username photo-url uid]} @db/fb-user]
        [:div.screen
         [header {:title "Clojure Learning Group"
                  :right [avatar {:src photo-url :username username}]}]
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


(defn profile [{:keys [username]}]
  (let [user (db/get-user-info username)
        {:keys [username fullname avatar-url bio]} user]
    [:div.screen.profile
     [header {:title (at username)
              :left  [back-to-chat]}]
     [:div.content
      (when (some? @db/bg-url)
        { :style { :background-image @db/bg-url }})
      [avatar {:src avatar-url :size :avatar-xl}]
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

         [:button.button ;; http://jsfiddle.net/dhyzV/1556/
          {:on-click #(-> (.getElementById js/document "background-image") .click)}
          "Upload background"]
         [:button.button {:on-click #(fb/sign-out)} "sign out"]])]]))


(defn nothing []
  [:div.screen
   [header {:title "Nothing is here!"
            :left  [back-to-chat]}]
   [:div.content
    (when (some? @db/bg-url)
         { :style { :background-image @db/bg-url }})]])


(defn app []
  (let [{:keys [screen params]} @db/route]
    (case screen
      :sign-in [sign-in]
      :chat    [chat params]
      :profile [profile params]
      [nothing])))