(ns clj-chat.views
  (:require [clj-chat.subs :as subs]
            [clj-chat.events :as evns]
            [reagent.core :as reagent]
            [clojure.string :as string]
            [clj-chat.routing :as routing]
            [accountant.core :as accountant]))


(defn- at [s] (str "@" s))


(defn- ^boolean me? [username]
  (= username (:username (subs/<sub [::subs/user]))))


(defn- local-date [date-str]
  (.toLocaleString (js/Date. date-str)))


(defn- scroll-to-bottom [this]
  (let [node (reagent/dom-node this)
        scroll-top (or (subs/<sub [::subs/scroll-top])
                       (.-scrollHeight node))
        opts #js {:top scroll-top :behavior "instant"}]
    (.scrollBy node opts)))


(defn- keep-scroll-position [this]
  (let [node (reagent/dom-node this)
        scroll-top (.-scrollTop node)]
    (evns/fire [::evns/write-to [:scroll-top] scroll-top])))  


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
   [avatar {:src (:avatar-url (subs/<sub [::subs/userinfo user]))
            :username user}]
   [:div.message-buble
    [:div.message-meta
     [:div.message-user (at user)]
     [:div.message-time (local-date time)]]
    [:div.message-text body]]])


(defn messages-list [chatname]
  (reagent/create-class
    {:display-name "messages-list"
     :component-did-mount scroll-to-bottom
     :component-did-update scroll-to-bottom
     :component-will-unmount keep-scroll-position
     :reagent-render (fn []
                       (let [bg-url (subs/<sub [::subs/bg-url])]
                         [:div.content
                          (when (some? bg-url) {:style {:background-image bg-url}})
                          (doall
                            (for [[key msg] (subs/<sub [::subs/sorted-messages chatname])]
                              ^{:key key} [message msg]))]))}))


(defn back-to-chat []
  [:small [:a {:href (routing/path-for :chat :chatname "clj-group")} "back to chat"]]) ;; FIXME



(defn my-buttons []
  [:div
   [:input#background-image
    {:type "file" 
     :accept "image/png, image/jpeg" 
     :on-change #(let [target (.-currentTarget %)
                       file   (-> target .-files (aget 0))]
                   (if (> (.-size file) 5242880)
                     (js/alert "File size shouldn't be greater than 5 MB!")
                     (evns/fire [::evns/upload-file file (:uid (subs/<sub [::subs/user]))]))
                   (set! (.-value target) ""))}]
   [:button.button ;; http://jsfiddle.net/dhyzV/1556/
    {:on-click #(-> (.getElementById js/document "background-image") .click)}
    "Upload background"]
   [:button.button {:on-click #(evns/fire [::evns/sign-out])} "sign out"]])


;; Main components
(defn sign-in []
  [:div.screen.sign-in
   [:button.button 
    {:on-click #(evns/fire [::evns/sign-in])}
    "Sign In with GitHub"]])


(defn chat [_]
  (let [input (reagent/atom "")]
    (fn [{:keys [chatname]}]
      (let [{:keys [username photo-url uid]} (subs/<sub [::subs/user])]
        [:div.screen
         [header {:title "Clojure Learning Group"
                  :right [avatar {:src photo-url :username username}]}]
         [messages-list chatname]
         [:div.footer
          [:textarea.input {:value @input
                            :on-change #(reset! input (.. % -target -value))}]
          [:button.button
           {:on-click #(when-not (empty? (string/trim @input))
                         (evns/fire [::evns/send-message chatname username uid @input])
                         (reset! input ""))}
           "Send"]]]))))


(defn profile [{:keys [username]}]
  (let [userinfo (subs/<sub [::subs/userinfo username])
        {:keys [username fullname avatar-url bio]} userinfo
        bg-url (subs/<sub [::subs/bg-url])]
    [:div.screen.profile
     [header {:title (at username)
              :left  [back-to-chat]}]
     [:div.content
      (when (some? bg-url) {:style {:background-image bg-url}})
      [avatar {:src avatar-url :size :avatar-xl}]
      [:div.profile-info
       [:div.username (at username)]
       [:div.full-name fullname]
       [:div.bio bio]]
      (when (me? username) [my-buttons])]]))


(defn nothing []
  [:div.screen
   [header {:title "Nothing is here!"
            :left  [back-to-chat]}]
   [:div.content]])


(defn app []
  (let [{:keys [screen params]} (subs/<sub [::subs/route])]
    (case screen
      :sign-in [sign-in]
      :chat    [chat params]
      :profile [profile params]
      [nothing])))