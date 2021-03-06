;; Copyright (c) 2020-2021 Saidone

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
   (:content @app-state)])

(defn mount [el]
  (rdom/render [board] el))

(defn mount-app-element []
  (when-let [el (get-app-element)]
    (mount el)))

(mount-app-element)

(defn ^:after-load on-reload []
  (mount-app-element))

(defn version [] "1.2")
(aset js/window "version" wa-tor.core/version)
