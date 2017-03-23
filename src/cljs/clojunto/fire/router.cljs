(ns clojunto.fire.router
  (:require
   [posh.core :as posh :refer [posh!]]
   [datascript.core :as d]
   [clojunto.fire.auth :refer [user]]
   [cljs-time.format :as format]
   [cljs.tools.reader.edn :as edn]
   [clojunto.fire.core :as core :refer [db-ref save]]
   [cljs.core.async :as async]
   [taoensso.timbre :as log]
   [cljs.spec :as s]
   [cljs-time.core :as time :refer [now]]
   [reagent.core :as r])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]))


(def formatter (format/formatters :basic-date-time))

(defn save-snapshot [path tx conn]
  (core/save path (pr-str {:snapshot-key tx
                           :conn @conn})))


(def readers  {:readers d/data-readers})

(defn load-conn [path startk startval schema component]
  (let [ref (db-ref path)]
    (.. ref
        (once "value"
              (fn [x]
                (try
                  (if-let [{k :snapshot-key db :conn} (edn/read-string readers (.val x))]
                    (let [datoms (set (d/datoms db :eavt))
                          conn (d/conn-from-datoms datoms schema)]
                      (do (posh! conn)
                          (reset! startval conn)
                          (reset! startk k)
                          (log/debugf "this is the k %s" (pr-str k))
                          (log/debugf "this is the startval %s" (pr-str @startval))))
                    (save-snapshot path ":NEW" (d/create-conn schema))
                    )
                  (catch js/Object e
                    (js/console.log e)
                    (js/console.log "Odds are, your schema isn't working"))))))
    (r/create-class
     {:display-name "listener"
      :component-will-unmount
      (fn will-unmount-listener [this]
        (.off ref))
      :reagent-render
      (fn
        [path startk startval schema component & args]
        (into [component startk startval] args))})))



(defn on-rolling [path startk startval snappath component]
  (let [ref (db-ref path)]
    (if  (or (= ":NEW" startk)
             (nil? startk))
      (.. ref
          (on "child_added"
              (fn [x]
                (let [val (.val x)
                      k (.getKey x)
                      parsed-val (edn/read-string {:readers d/data-readers} val)
                      tx (:tx parsed-val)]
                  (do
                    (log/debugf "running on all child-added %s" (pr-str parsed-val))
                    (d/transact! @startval tx)
                    (save-snapshot snappath k @startval))))))
      (.. ref
          orderByKey
          (startAt (clj->js startk))
          (on "child_added"
              (fn [x]
                (let [val (.val x)
                      k (.getKey x)]
                  (try
                   (if-let [parsed-val (edn/read-string {:readers d/data-readers} val)]
                      (do
                        (d/transact! @startval (:tx parsed-val))
                        (save-snapshot snappath k @startval) )
                      (println "Bad parse" val))
                      (catch js/Object e
                        (println e val)))
                  )))))
    (r/create-class
     {:display-name "listener"
      :component-will-unmount
      (fn will-unmount-listener [this]
        (.off ref))
      :reagent-render
      (fn
        [path startk startval latestsnap component & args]
        (into [component startval latestsnap] args))})))



(defn transact! [path tx]
  (try   (core/push path (pr-str {:user/email (:email @user nil)
                                  :tx tx
                                  :time (format/unparse formatter (now))}))

         (catch js/Object e
           (println e))))






(defmulti handle-event (fn [ctx [ev-type & _]] ev-type))
(defmethod handle-event :default
  [_ msg]
  (log/debugf "Unhandled event: %s" (pr-str msg))
  nil)



(defn dispatch [ch msg]
  (log/debugf "Dispatching event: %s" (pr-str msg))
  (async/put! ch msg))


(defn handle-event* [{:as router-ctx :keys [conn ev-chan transact]} msg]
  (let [ev-ctx (assoc router-ctx :db @conn)
        {:as ret :keys [tx]} (handle-event ev-ctx msg)]
    (log/tracef "Handler returned: %s" (pr-str ret))
    (when tx
      (log/debugf "Transacting: %s" (pr-str tx))
      (transact tx))
    (doseq [new-msg (:dispatch ret)]
      (dispatch ev-chan new-msg))
    (doseq [[timeout new-msg] (:dispatch-later ret)]
      (go
        (async/<! (async/timeout timeout))
        (dispatch ev-chan new-msg)))
    ))


(defn start [{:as router-ctx :keys [ev-chan]}]
  (go-loop [msg (<! ev-chan)]
    (when msg
      (try
        (log/debugf "Handling event: %s" (pr-str msg))
        (handle-event* router-ctx msg)
        (catch :default ex
          (log/errorf ex "Error handling event: %s" (pr-str msg))))
      (recur (<! ev-chan)))))




(defn mounter [{:keys [snappath
                       txpath
                       startk
                       connatom
                       schema
                       email
                       loading
                       main]
                :as ctx}]
  (r/with-let [ev-chan (async/chan (async/dropping-buffer 10))]
    [load-conn snappath startk connatom schema
     (fn [k connatom]
       (if @k
         [on-rolling txpath @k connatom snappath 
          (fn [connatom]
            (r/with-let [new-ctx (merge ctx {:conn @connatom
                                             :email email
                                             :transact (partial transact! txpath)
                                             :ev-chan ev-chan
                                             :dispatch (partial dispatch ev-chan)})
                         _ (start new-ctx)]
              [main
               new-ctx]))]
         [loading]))]
    (finally
      (async/close! ev-chan))))






