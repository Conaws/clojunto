(ns clojunto.core
  (:require-macros [secretary.core :refer [defroute]])
  (:import goog.History)
  (:require
   [secretary.core :as secretary]
   [goog.events :as events]
   [goog.history.EventType :as EventType]
   [clojunto.fire.auth :as auth :refer [user]]
   [recalcitrant.core :refer [error-boundary]]
   [reagent.core :as reagent]
   [clojunto.routes.home.page :as home]
   [devtools.core :as devtools]
   ))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Vars

(defonce debug?
  ^boolean js/goog.DEBUG)

(defonce app-state
  (reagent/atom
   {:text "Hello, what is your name? "
    :page :nil}))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Routes

(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

(defn app-routes []
  (secretary/set-config! :prefix "#")

  (defroute "/" []
    (swap! app-state assoc :page :home))

  (defroute "/about" []
    (swap! app-state assoc :page :about))

  ;; add routes here


  (hook-browser-navigation!))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Pages

(defn home [ratom]
  (let [text (:text @ratom)]
    [:div [:h1 "Home Page"]
     [:p text "FIXME"]
     [:a {:href "#/about"} "about page"]]))


(defn landing []
  [rc/v-box
   :align :center
   :justify :center
   :gap "32px"
   :children
   [[rc/title :label "Welcome to Clojunto" :level :level1]
    [:div {:style {:max-width "350px"
                   :text-align "center"}}
     "This is a tool to help small groups or Clojurians self organize around intersecting interests and availability."
     ]
    [:div {:style {:max-width "350px"
                   :text-align "center"}}
     "Tag yourself with as many interests as you like, say what times you're generally available to meet with someone else in the network (online or offline) and your timezone, then discover hangouts that are already planned that match your interests, or explore the directory and see combinations of interests"
     ]]])


(defn home [ratom]
  (if-let [{:keys [display-name email]} @user]
    [home/page email]
    [landing]))





(defn about [ratom]
  [:div [:h1 "About Page"]
   [:a {:href "#/"} "home page"]])



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Initialize App

(defmulti page identity)
(defmethod page :home [] home)
(defmethod page :about [] about)
(defmethod page :default [] (fn [_] [:div]))

(defn current-page [ratom]
  (let [page-key (:page @ratom)]
    [(page page-key) ratom]))

(defn dev-setup []
  (when debug?
    (enable-console-print!)
    (println "dev mode")
    (devtools/install!)
    ))

(defn reload []
  (reagent/render-component (fn [] [error-boundary
                                    [current-page app-state]
                                    ])
                            (.getElementById js/document "app"))

  )

(defn ^:export main []
  (dev-setup)
  (app-routes)
  (reload))
