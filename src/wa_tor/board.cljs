(ns wa-tor.board
  (:require
   [reagent.core :as reagent :refer [atom]]
   [wa-tor.logic :as logic]))

(defonce window-width  (.-innerWidth js/window))
(defonce window-height  (.-innerHeight js/window))

(defonce blocksize 16)

(defonce board (atom {}))

(defonce state (reagent/atom {}))

(swap! board assoc :w (quot (* .92 window-width) blocksize))
(swap! board assoc :h (quot (* .94 window-height) blocksize))

(defonce area (* (:w @board) (:h @board)))

;; initial parameters as in the original article
(swap! board assoc :nfish (quot area 2.25))
(swap! board assoc :nsharks (quot area 22.5))
(swap! board assoc :fbreed 3)
(swap! board assoc :sbreed 10)
(swap! board assoc :starve 3)

(defn- randomize-board! []
  (swap! board assoc :board (logic/populate-board! (dissoc @board :board))))

(defn- toggle-modal []
  (-> (.getElementById js/document "usage") (aget "classList") (.toggle "show-modal")))

(defn- toggle [id]
  (if (:start @state)
    (toggle-modal)
    (let [type (:type (get (:board @board) id))]
      (cond
        (= type 'fish) (swap! board assoc :board (assoc (:board @board) id {:type 'shark :age 0 :starve 0}))
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
    [:span {:class "close-button"
            :onClick #(toggle-modal)} "[X]"]
    [:b [:pre "   USAGE"]]
    "Pause the game to edit board, either by" [:br]
    "pressing spacebar" [:br]
    "or" [:br]
    "tapping with two fingers" [:br] [:br]
    "click on a square to cycle between" [:br]
    "sea >>> fish >>> shark" [:br] [:br]
    [:div
     "Number of fish: " [:b (:nfish @board)] [:br]
     [slider :nfish (:nfish @board) 0 (- area (:nsharks @board))]]
    [:div
     "Number of sharks: " [:b (:nsharks @board)] [:br]
     [slider :nsharks (:nsharks @board) 0 (- area (:nfish @board))]]
    [:div
     "Fish breed time: " [:b (:fbreed @board)] " chronons" [:br]
     [slider :fbreed (:fbreed @board) 1 20]]
    [:div
     "Shark breed time: " [:b (:sbreed @board)] " chronons" [:br]
     [slider :sbreed (:sbreed @board) 1 20]]
    [:div
     "Shark starve after: " [:b (:starve @board)] " chronons w/o food" [:br]
     [slider :starve (:starve @board) 1 20]]
    "Other commands:" [:br]
    "\"c\" or swipe left to clear board " [:b "*and*"] " pause" [:br]
    "\"r\" or swipe right to randomize board" [:br]
    "\"h\" or swipe up to toggle this panel" [:br]
    [:hr]
    "More on " [:a {:href "https://github.com/saidone75/wa-tor/blob/master/wator_dewdney.pdf"} "Wa-Tor"] [:br]
    "You can grab the source code " [:a {:href "https://github.com/saidone75/wa-tor"} "here"] [:br]
    "Copyright (c) 2020 " [:a {:href "https://saidone.org"} "Saidone"] [:br]
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
             (loop [board board blocks '()]
               (if (empty? board)
                 blocks
                 (recur (rest board)
                        (let [k (key (first board)) v (val (first board))]
                          (conj blocks ^{:key k} [block k
                                                  (* blocksize (mod k w))
                                                  (* blocksize (quot k w))
                                                  (cond
                                                    (= 'fish (:type v)) "gold"
                                                    (= 'shark (:type v)) "lightslategray"
                                                    :else "aqua")])))))]])))

(defn- clear-board! []
  (swap! state assoc :start false)
  (swap! board assoc :board
         (apply merge (for [x (range area)]
                        (array-map x nil)))))

(defn- update-board! []
  (if (:start @state)
    (let [prev-board (logic/sh-fi (:board @board))]
      (swap! board assoc :board (logic/next-chronon @board))
      (if (and (= (first prev-board) (first (logic/sh-fi (:board @board))))
               (= (last prev-board) (last (logic/sh-fi (:board @board))))
               (not (= (count (first prev-board)) area)))
        (swap! state assoc :start false)))))

(defn- keydown-handler [event]
  (if (.getElementById js/document "board")
    (cond
      (= 72 event.keyCode) (toggle-modal)
      (= 32 event.keyCode) (swap! state assoc :start (not (:start @state)))
      (= 67 event.keyCode) (clear-board!)
      (= 82 event.keyCode) (randomize-board!))))

(defonce touchstart {})
(defonce swipe-threshold (/ window-width 3))
(defonce time-threshold {:min 180 :max 1000})

(defn- touchstart-handler [event]
  (if (.getElementById js/document "board")
    (cond
      (= 2 event.touches.length) (swap! state assoc :start (not (:start @state)))
      :else (set! touchstart {:x (-> event.changedTouches (aget 0) (aget "pageX"))
                              :y (-> event.changedTouches (aget 0) (aget "pageY"))
                              :t (.getTime (js/Date.))}))))

(defn- touchend-handler [event]
  (let [touchend {:x (-> event.changedTouches (aget 0) (aget "pageX"))
                  :y (-> event.changedTouches (aget 0) (aget "pageY"))
                  :t (.getTime (js/Date.))}
        xdistance (- (:x touchend) (:x touchstart))
        ydistance (- (:y touchend) (:y touchstart))
        time (- (:t touchend) (:t touchstart))]
    (if (and (> time (:min time-threshold)) (< time (:max time-threshold)))
      (cond
        (< xdistance (* -1 swipe-threshold)) (clear-board!)
        (> xdistance swipe-threshold) (randomize-board!)
        (< ydistance (* -1 swipe-threshold)) (toggle-modal)))))

(defn create-board! []
  (if (nil? (:board @board))
    (do
      (add-watch board :board #(draw-board))
      (js/document.addEventListener "keydown" keydown-handler)
      (js/document.addEventListener "touchstart" touchstart-handler)
      (js/document.addEventListener "touchend" touchend-handler)      
      (randomize-board!)
      (swap! state assoc :start true)
      (swap! state assoc :interval (js/setInterval update-board! 200))))
  state)
