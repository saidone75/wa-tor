;; Copyright (c) 2020-2026 Saidone

(ns ^:figwheel-hooks wa-tor.core
  (:require
   [goog.dom :as gdom]
   [reagent.dom :as rdom]
   [wa-tor.board :as board]))

(defonce app-state (board/create-board!))

(defn get-app-element []
  (gdom/getElement "app"))

(defn board []
  [:div {:class "outer-div"}
   [:div.board {:id "board"}
    (board/splash)
    (board/usage-panel)
    (board/stats-panel)
    (board/draw-board)]])

(defn mount [el]
  (rdom/render [board] el))

(defn mount-app-element []
  (when-let [el (get-app-element)]
    (mount el)))

(mount-app-element)

(defn ^:after-load on-reload []
  (mount-app-element))

(defn version [] "1.5-SNAPSHOT")
(aset js/window "version" wa-tor.core/version)
