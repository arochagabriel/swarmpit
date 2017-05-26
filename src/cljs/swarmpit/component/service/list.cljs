(ns swarmpit.component.service.list
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [clojure.string :as string]
            [rum.core :as rum]
            [sablono.core :refer-macros [html]]))

(enable-console-print!)

(def cursor [:form :service :list :filter])

(def headers ["Name" "Image" "Mode" "Replicas" "Status" "Updates"])

(def list-replicas-style
  {:backgroundColor "rgb(245, 245, 245)"
   :border          "1px solid rgb(224, 228, 231)"})

(def list-replicas-label-style
  {:fontSize "13px"})

(def list-updates-style
  {:display  "inline-block"
   :position "relative"})

(defn list-replicas [value]
  (comp/chip {:style      list-replicas-style
              :labelStyle list-replicas-label-style} value))

(defn list-updates [value]
  (let [status (if value "loading" "ready")]
    (comp/refresh-indicator
      {:size   30
       :left   8
       :top    0
       :status status
       :style  list-updates-style})))

(defn- filter-items
  "Filter list `items` based on `name` & `cranky?` flag"
  [items name cranky?]
  (let [is-running (fn [item] (= "running" (:state item)))
        is-updating (fn [item] (get-in item [:status :update]))]
    (if cranky?
      (filter #(and (string/includes? (:serviceName %) name)
                    (and (not (is-running %))
                         (not (is-updating %)))) items)
      (filter #(string/includes? (:serviceName %) name) items))))

(defn- render-item
  [item]
  (let [value (val item)]
    (case (key item)
      :state (case value
               "running" (comp/label-green value)
               "not running" (comp/label-grey value)
               "partial running" (comp/label-yellow value))
      :info (list-replicas value)
      :update (list-updates value)
      value)))

(rum/defc service-list < rum/reactive [items]
  (let [{:keys [serviceName cranky]} (state/react cursor)
        filtered-items (filter-items items serviceName cranky)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-text-field
         {:hintText "Filter by name"
          :onChange (fn [_ v]
                      (state/update-value :serviceName v cursor))})
       [:span.form-panel-space]
       (comp/panel-comp
         "Show cranky services"
         (comp/checkbox
           {:checked cranky
            :onCheck (fn [_ v]
                       (state/update-value :cranky v cursor))}))]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:href    "/#/services/create"
            :label   "Create"
            :primary true}))]]
     (comp/list-table headers
                      filtered-items
                      render-item
                      [[:serviceName] [:image] [:mode] [:status :info] [:state] [:status :update]]
                      "/#/services/")]))

(defn- init-state
  []
  (state/set-value {:serviceName ""
                    :cranky      false} cursor))

(defn mount!
  [items]
  (init-state)
  (rum/mount (service-list items) (.getElementById js/document "content")))