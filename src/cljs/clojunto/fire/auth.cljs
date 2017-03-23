(ns clojunto.fire.auth
  (:require [cljsjs.firebase]
            [re-com.core :as rc]
            [reagent.core :as r]))

(defonce user (r/atom nil))


(defn sign-in-with-popup
  ([error-atom]
   (do 
     (.catch (.signInWithPopup
              (js/firebase.auth.)
              (js/firebase.auth.GoogleAuthProvider.))
             (fn [error]
               (reset! error-atom error))
             )))
  ([]
   (do 
     (.signInWithPopup
      (js/firebase.auth.)
      (js/firebase.auth.GoogleAuthProvider.)))))


(def sign-in sign-in-with-popup)

(defn sign-in-with-redirect
  ([]
   ;; TODO: use Credential for mobile.
   (do
     (.signInWithRedirect
      (js/firebase.auth.)
      (js/firebase.auth.GoogleAuthProvider.)))))


(defn sign-out []
  (do
    (.signOut (js/firebase.auth))
    (reset! user nil)))



(defn sign-in-with-email [email password response]
  (.catch
   (.signInWithEmailAndPassword
    (js/firebase.auth)
    email
    password)
   (fn [error]
     (reset! response (str error)))))

(defn sign-up
  ([email password] (sign-up email password nil))
  ([email password response]
   (.catch
    (.createUserWithEmailAndPassword
     (js/firebase.auth)
     email
     password)
    (fn [error]
      (reset! response (str error))))))



(defn user-button [display-name]
  [rc/h-box
   :gap "8px"
   :align :center
   :children [
              [:label display-name]
              [rc/button
               :label ""
               :tooltip "Sign Out"
               :on-click #(sign-out)
               :style {:background-image "url(http://dreamatico.com/data_images/kitten/kitten-1.jpg)"
                       :background-size "cover"
                       :height "36px"
                       :width "36px"
                       :border-radius "70px"
                       :background-repeat "no-repeat"}
               ]
              ]])


(defn login-view []
  [:div
   (if-let [{:keys [display-name]} @user]
     [user-button display-name]
     [:div
      [rc/button
       :on-click #(sign-in-with-popup)
       :label "Signin with Google"]])])
