(ns clojunto.routes.home.page
  (:require [re-com.core :as rc]
            [clojunto.fire.router :refer [mounter]))

(defonce schema {:user/email {:db/unique :db.unique/identity}
                 :user/name {:db/unique :db.unique/identity}
                 :user/interests {:db/valueType :db.type/ref :db/cardinality :db.cardinality/many}
                 :user/wants.to.learn {:db/valueType :db.type/ref :db/cardinality :db.cardinality/many}
                 :user/can.teach {:db/valueType :db.type/ref :db/cardinality :db.cardinality/many}
                 :interest/title {:db/unique :db.unique/identity}
                 :tag.time/dayhour {:db/unique :db.unique/identity}
                 :user/availability {:db/valueType :db.type/ref :db/cardinality :db.cardinality/many}
                 :user/onboarding.completed {:db/cardinality :db.cardinality/many}
                 :meeting/dayhour {:db/valueType :db.type/ref} ;;; unsure about this
                 :meeting/topics {:db/valueType :db.type/ref :db/cardinality :db.cardinality/many}
                 :meeting/confirmed-attendees {:db/valueType :db.type/ref :db/cardinality :db.cardinality/many}
                 :meeting/proposed-by {:db/valueType :db.type/ref}
                 })



(defn loading []
  [rc/v-box
   :align :center
   :children [[rc/title :label "Stop." :level :level1]
              [rc/title :label "Hammock Time." :level :level1]
              [:p "If you don't get the reference, go watch "
               [:a {:href "https://www.youtube.com/watch?v=f84n5oFoZBc" :target "_blank"} "this talk "] "now"]
              [:p "If we really need to spell it out of you "
               [:a {:href "https://www.youtube.com/watch?v=otCpCn0l4Wo" :target "_blank"} "... well"] ]
              [rc/throbber :size :large]]])


(defn main [{:keys [conn dispatch email]}]
;; todo, put errthing in here
;; the heart of the application is
  )


(defn page [email]
  [mounter
   {:snappath ["re-kindle""snapshot"]
    :txpath ["re-kindle""txpath"]
    :main main
    :schema schema
    :loading loading
    :connatom (r/atom nil)
    :startk (r/atom nil)
    :email email}])

