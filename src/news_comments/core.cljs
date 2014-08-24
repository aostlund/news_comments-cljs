(ns comments.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [<! timeout]]
            [cljs-http.client :as http])
  (:import [goog.net XhrIo]))

(enable-console-print!)
(def post-id (.getAttribute (.getElementById js/document "post_id") "data"))
(def base-url (str "http://" (.-host (.-location js/window))))
(def json-url (str base-url "/posts/" post-id ".json"))

(def app-state (atom [{:name "Dexrion" :message "test message"} {:name "Sork pippi" :message "this is another test message"}]))
(def user (atom {:user "not logged in"}))

(defn read-data [] (.send XhrIo json-url
  (fn [res] (reset! app-state (js->clj (.getResponseJson (.-target res)) :keywordize-keys true))))
  (.send XhrIo (str base-url "/shouts/show.json")
  (fn [res] (reset! user (js->clj (.getResponseJson (.-target res)) :keywordize-keys true)))))

(read-data)

(defn update-fn [data owner]
  (reset! app-state (into  @app-state [{:name (.-value (om/get-node owner "name")) :message (.-value (om/get-node owner "message"))}]))
  (http/post (str base-url "/comments") {:json-params {:post post-id :comments (last @app-state)}})
  (set! (.-value (om/get-node owner "message")) ""))

(defn delete-comment [id]
  (http/delete (str base-url "/comments/" id) {:json-params {:_method "delete"}}))

(defn comment-form [state owner]
  (reify
    om/IRender
    (render [this]
      (dom/div nil
        (dom/input #js {:name "utf8" :type "hidden" :value "true"})
        (dom/textarea #js {:style #js {:width "97%" :height "70px"} :id "comment_message" :name "comment[message]" :row "3" :ref "message"})
        (dom/p #js {:style #js {:color "#FBB600" :width: "24%" :display "inline" :margin "2%"}} "Name:")
        (dom/input #js {:id "comment_name" :name "comment[name]" :type "text" :ref "name" :style #js {:width "50%"}})
        (dom/input #js {:name "commit" :onClick #(update-fn state owner) :type "submit" :value "Submit" :style #js {:width "24%"}})))))

(defn comment [state owner]
  (dom/tr nil
    (dom/td #js {:style #js {:color "#FBB600" :fontSize "small"}} (str (:name state) ":"))
    (dom/td #js {:style #js {:fontSize "small"}} (:message state))
    (if (= (:user @user) "admin")
      (dom/td #js {:style #js {:fontSize "xx-small"} :onClick #(delete-comment (:id @state))} "Delete"))))

(defn comments [state owner]
  (apply dom/tbody nil
    (om/build-all comment state)))

(defn comment-box [state owner]
  (dom/div #js {:className "b_box"}
    (dom/div nil
      (dom/table nil
        (om/build comments state)))
    (om/build comment-form state)))

(om/root comment-box
  app-state
  {:target (. js/document (getElementById "comments"))})

(go (while true
  (<! (timeout 5000))
  (read-data)))
