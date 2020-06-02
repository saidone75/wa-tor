(ns wa-tor.board
  (:require
   [reagent.core :as reagent :refer [atom]]
   [wa-tor.logic :as logic]))

(defonce window-width  (.-innerWidth js/window))
(defonce window-height  (.-innerHeight js/window))

(defonce blocksize 16)

(def board (atom {}))

(defonce state (reagent/atom {}))

(swap! board assoc :w (quot (* .90 window-width) blocksize))
(swap! board assoc :h (quot (* .85 window-height) blocksize))

(defn- block [id x y color]
  [:rect {:id id
          :x x
          :y y
          :fill color
          ;;:on-click #(toggle id)
          :width "14px"
          :height "14px"
          }])

(defn- draw-board []
  (let [{w :w h :h board :board} @board]
    (swap! state assoc :content
           [:div.board {:id "board"}
            ;;(modal)
            [:svg.board {:width (* blocksize w) :height (* blocksize h)}
             (loop [board board blocks '() i 0]
               (if (empty? board) blocks
                   (recur (rest board)
                          (conj blocks ^{:key i} [block i
                                                  (* blocksize (mod i w))
                                                  (* blocksize (quot i w))
                                                  (cond
                                                    (= 'fish (:type (first board))) "gold"
                                                    (= 'shark (:type (first board))) "lightslategray"
                                                    :else "aqua")])
                          (inc i))))]])))

(defn- update-board! []
  (if (:start @state)
    (let [prev-board (logic/sh-fi (:board @board))]
      (swap! board assoc :board (logic/next-chronon @board))
      (if (= prev-board (logic/sh-fi (:board @board)))
        (do
          (swap! state assoc :start false)
          (let [area (* (:w @board) (:h @board))]
            (js/setTimeout #(do
                              (swap! state assoc :start true)
                              (swap! board assoc :board (logic/populate-board @board (quot area 10) (quot area 10)))) 5000)))))))

(defn create-board! []
  (if (nil? (:board @board))
    (do
      (swap! board assoc :shark-energy 6)
      (swap! board assoc :shark-breed 12)
      (swap! board assoc :fish-breed 5)
      (add-watch board :board #(draw-board))
      (let [area (* (:w @board) (:h @board))]
        (swap! board assoc :board (logic/populate-board @board (quot area 10) (quot area 10))))
      (swap! state assoc :start true)
      (swap! state assoc :interval (js/setInterval update-board! 200) :speed 1)))
  state)
