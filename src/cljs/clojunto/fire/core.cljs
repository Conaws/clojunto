(ns clojunto.fire.core
  (:require [cljsjs.firebase]
            [clojure.string :as string]
            [reagent.core :as r]
            [clojunto.fire.auth :as auth]
            [reagent.core :as r]))

(def timestamp
  js/firebase.database.ServerValue.TIMESTAMP)

(defn db-ref [path]
  (.ref (js/firebase.database) (string/join "/" path)))

(defn save [path value]
  (.set (db-ref path) value))

(defn push [path value]
  (.push (db-ref path) value))

(defn on-state-changed [user-atom]
  (.onAuthStateChanged
   (js/firebase.auth)
   (fn auth-state-changed [user-obj]
     (let [uid (.-uid user-obj)
           display-name (.-displayName user-obj)
           photo-url (.-photoURL user-obj)
           email (.-email user-obj)
           provider-data (.-providerData user-obj)]
       (if uid
         (do
           (save ["users" uid "settings"]
                 #js {:photo-url photo-url
                      :display-name display-name
                      :email email})
           (reset! user-atom {:photo-url photo-url
                              :display-name display-name
                              :uid uid
                              :provider-data provider-data
                              :email email
                              :obj (js->clj user-obj)}))
         (when @user-atom
           (do    (reset! user-atom nil)
                  (reset! auth/loading false))))))
   (fn auth-error [error]
     (js/console.log error))))


(defn init! []
  (js/firebase.initializeApp
   #js {:apiKey "AIzaSyDEtDZa7Sikv7_-dFoh9N5EuEmGJqhyK9g"
        :authDomain "firescript-577a2.firebaseapp.com"
        :databaseURL "//firescript-577a2.firebaseio.com"
        :storageBucker ""})
  (on-state-changed auth/user))

(init!)




