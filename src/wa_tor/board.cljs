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

(swap! board assoc :nfishes (quot (* (:w @board) (:h @board)) 10))
(swap! board assoc :nsharks (quot (* (:w @board) (:h @board)) 10))

(swap! board assoc :fish-breed 5)
(swap! board assoc :shark-energy 6)
(swap! board assoc :shark-breed 12)

(defn- randomize-board []
  (let [area (* (:w @board) (:h @board))]
    (swap! board assoc :board (logic/populate-board @board (:nfishes @board) (:nsharks @board)))))

(defn- toggle-modal []
  (-> (.getElementById js/document "usage") (aget "classList") (.toggle "show-modal")))

(defn- toggle [id]
  (if (not (:start @state))
    (let [type (:type (get (:board @board) id))]
      (cond
        (= type 'fish) (swap! board assoc :board (assoc (:board @board) id {:type 'shark :age 0 :energy (:shark-energy @board)}))
        (= type 'shark) (swap! board assoc :board (assoc (:board @board) id nil))
        :else (swap! board assoc :board (assoc (:board @board) id {:type 'fish :age 0}))))))

(defn slider [param value min max]
  [:input {:type "range" :value value :min min :max max
           :style {:width "80%"}
           :onChange (fn [e]
                       (let [new-value (js/parseInt (.. e -target -value))]
                         (swap! board assoc param new-value)))}])

(defn- modal []
  [:div.modal {:id "usage"}
   [:div.modal-content {:class (let [ratio (/ window-width window-height)]
                                 (if (> ratio 1)
                                   "modal-content-large"
                                   "modal-content-small"))}
    [:b "USAGE"] [:br]
    "Pause the game to edit board by pressing spacebar" [:br]
    "then click on a square to cycle between sea>fish>shark" [:br] [:br]
    [:div
     "Number of fishes: " [:b (:nfishes @board)] [:br]
     [slider :nfishes (:nfishes @board) 0 (- (* (:w @board) (:h @board)) (:nsharks @board))]]
    [:div
     "Number of sharks: " [:b (:nsharks @board)] [:br]
     [slider :nsharks (:nsharks @board) 0 (- (* (:w @board) (:h @board)) (:nfishes @board))]]
    [:div
     "Fish breed time: " [:b (:fish-breed @board)] " chronons" [:br]
     [slider :fish-breed (:fish-breed @board) 1 20]]
    [:div
     "Shark breed time: " [:b (:shark-breed @board)] " chronons" [:br]
     [slider :shark-breed (:shark-breed @board) 1 20]]
    [:div
     "Shark energy: " [:b (:shark-energy @board)] [:br]
     [slider :shark-energy (:shark-energy @board) 1 20]]
    "Other commands:" [:br]
    "\"c\" to clear board " [:b "*and*"] " pause" [:br]
    "\"r\" to randomize board" [:br]
    "\"h\" to toggle this panel" [:br]
    [:hr]
    "More on " [:a {:href "https://en.wikipedia.org/wiki/Wa-Tor"} "Wa-Tor"] [:br]
    "You can grab the source code " [:a {:href "https://github.com/saidone75/wa-tor"} "here"] [:br]
    "Copyright (c) 2020 Saidone" [:br]
    "Distributed under the " [:a {:href "https://github.com/saidone75/wa-tor/blob/master/LICENSE"} "MIT License"]]])

(defn- block [id x y color]
  [:rect {:id id
          :x x
          :y y
          :fill color
          :on-click #(toggle id)
          :width "14px"
          :height "14px"
          }])

(defn- draw-board []
  (let [{w :w h :h board :board} @board]
    (swap! state assoc :content
           [:div.board {:id "board"}
            (modal)
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

(defn- clear-board []
  (let [{w :w h :h} @board]
    (swap! state assoc :start false)
    (swap! board assoc :board (vec (take (* w h) (repeat nil))))))

(defn- update-board! []
  (if (:start @state)
    (let [prev-board (logic/sh-fi (:board @board))]
      (swap! board assoc :board (logic/next-chronon @board))
      (if (= prev-board (logic/sh-fi (:board @board)))
        (swap! state assoc :start false)))))

(defn- keydown-handler [event]
  (if (.getElementById js/document "board")
    (cond
      (= 72 event.keyCode) (toggle-modal)
      (= 32 event.keyCode) (swap! state assoc :start (not (:start @state)))
      (= 67 event.keyCode) (clear-board)
      (= 82 event.keyCode) (randomize-board))))

(defn create-board! []
  (if (nil? (:board @board))
    (do
      (add-watch board :board #(draw-board))
      (js/document.addEventListener "keydown" keydown-handler)
      (let [area (* (:w @board) (:h @board))]
        (swap! board assoc :board (logic/populate-board @board (quot area 10) (quot area 10))))
      (swap! state assoc :start true)
      (swap! state assoc :interval (js/setInterval update-board! 200) :speed 1)))
  state)
