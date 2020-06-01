(ns ^:figwheel-hooks wa-tor.core
  (:require
   [goog.dom :as gdom]
   [reagent.core :as reagent :refer [atom]]
   [wa-tor.board :as board]))

(defonce app-state (board/create-board))

(defn get-app-element []
  (gdom/getElement "app"))

(defn board []
  [:div {:class "outer-div"}
   (:content @app-state)])

(defn mount [el]
  (reagent/render-component [board] el))

(defn mount-app-element []
  (when-let [el (get-app-element)]
    (mount el)))

(mount-app-element)

(defn ^:after-load on-reload []
  (mount-app-element))
