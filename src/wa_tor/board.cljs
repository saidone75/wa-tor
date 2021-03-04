;; Copyright (c) 2020-2021 Saidone

(ns wa-tor.board
  (:require
   [reagent.core :as reagent :refer [atom]]
   [wa-tor.logic :as logic]))

(defonce window-width  (.-innerWidth js/window))
(defonce window-height  (.-innerHeight js/window))

(defonce blocksize 16)

(defonce board (atom {}))

(defonce state (reagent/atom {}))

;; original ocean size was:
;; 80x23 on Magi's VAX
;; 32x14 on A.K. Dewdney's IBM PC
(swap! board assoc :w (quot (* .92 window-width) blocksize))
(swap! board assoc :h (quot (* .94 window-height) blocksize))

(defonce area (* (:w @board) (:h @board)))

;; initial parameters as in the original article
(swap! board assoc :nfish (quot area 2.25))
(swap! board assoc :nsharks (quot area 22.5))
(swap! board assoc :fbreed 3)
(swap! board assoc :sbreed 10)
(swap! board assoc :starve 3)
;; extra randomness on by default
(swap! board assoc :random true)

;; history for stats
(defonce history-size 500)
;; default stats window width
(swap! board assoc :history-window 200)
;; history buffer
(def history (vec (take history-size (repeat []))))
;; unique id for lines
(defonce line-id (atom 0))

;; chronon counter
(def chronon 0)
;; magnify sharks stats
(swap! board assoc :magnify-sharks 1)

(defn clear-stats! []
  (set! history (vec (take history-size (repeat []))))
  (set! chronon 0))

(defn- randomize-board! []
  (clear-stats!)
  (swap! board assoc :board (logic/populate-board! (dissoc @board :board))))

(defn- toggle-modal [id]
  (-> (.getElementById js/document id) (aget "classList") (.toggle "show-modal")))

(defn- show-stats []
  (if-not (= "modal show-modal" (-> (.getElementById js/document "usage") (aget "classList") (aget "value")))
    (-> (.getElementById js/document "stats") (aget "classList") (.add "show-modal"))))

(defn- toggle [id]
  (if (:start @state)
    (toggle-modal "usage")
    (let [type (:type (get (:board @board) id))]
      (cond
        (= type 'fish) (swap! board assoc :board (assoc (:board @board) id {:type 'shark :age 0 :starve 0}))
        (= type 'shark) (swap! board assoc :board (assoc (:board @board) id nil))
        :else (swap! board assoc :board (assoc (:board @board) id {:type 'fish :age 0}))))))

(defn slider [param value min max width step]
  [:input {:type "range" :value value :min min :max max
           :style {:width (if-not (nil? width) (str width "%") "80%")}
           :step (str step)
           :onChange (fn [e]
                       (let [new-value (js/parseInt (.. e -target -value))]
                         (swap! board assoc param new-value)))}])

(defn checkbox [param]
  [:input {:type :checkbox :checked (param @board)
           :onChange (fn [e]
                       (swap! board assoc param (not (param @board))))}])

(defn- modal []
  [:div.modal {:id "usage"}
   [:div.modal-content {:class (let [ratio (/ window-width window-height)]
                                 (if (> ratio 1)
                                   "modal-content-large"
                                   "modal-content-small"))}
    [:span {:class "close-button"
            :onClick #(toggle-modal "usage")} "[X]"]
    [:b [:pre "   USAGE"]]
    [:table {:class "usage"}
     [:tr [:td "pause/resume"] [:td "spacebar or two fingers tap"]]
     [:tr [:td "clear board"] [:td "\"C\" or swipe left"]]
     [:tr [:td "randomize board"] [:td "\"R\" or swipe right"]]
     [:tr [:td "toggle usage panel"] [:td "\"H\" or swipe up"]]
     [:tr [:td "show stats"] [:td "\"S\" or long touch (> 2s)"]]]
    [:br]
    "click on a square to cycle between" [:br]
    "sea >>> fish >>> shark" [:br] [:br]
    [:div
     "Initial number of fish: " [:b (:nfish @board)] [:br]
     [slider :nfish (:nfish @board) 0 (- area (:nsharks @board))]]
    [:div
     "Initial number of sharks: " [:b (:nsharks @board)] [:br]
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
    "Extra randomness: " [:b (str(:random @board))] [:br]
    "off"
    [:label {:class "switch"}
     [checkbox :random]
     [:span {:class "slider"}]]
    "on"
    [:br]
    [:hr]
    "More on " [:a {:href "https://github.com/saidone75/wa-tor/blob/master/wator_dewdney.pdf"} "Wa-Tor"] [:br]
    "You can grab the source code " [:a {:href "https://github.com/saidone75/wa-tor"} "here"] [:br]
    "Copyright (c) 2020-2021 " [:a {:href "https://saidone.org"} "Saidone"]
    [:hr]]])

(defn- stats-graph []
  [:svg.stats {:id "svg.stats" :width "100%" :height "100%"}
   (if (and (= "complete" (aget js/document "readyState"))
            (not (= "modal" (aget (.getElementById js/document "stats") "classList"))))
     (let [height (aget (.getElementById js/document "svg.stats") "clientHeight")
           width (aget (.getElementById js/document "svg.stats") "clientWidth")
           stepx (/ width (:history-window @board))
           sw 3]
       (reset! line-id 0)
       (conj
        (loop [history (take-last (:history-window @board) history) x 0 blocks '()]
          (if (< (count history) 2) blocks
              (recur (drop 1 history) (+ x stepx)
                     (conj blocks
                           ^{:key (swap! line-id inc)} [:line
                                                        {:x1 x
                                                         :x2 (+ x stepx)
                                                         :y1 (+ (- height sw) (* -1 (- height (* 2 sw)) (/ (first (first history)) area)))
                                                         :y2 (+ (- height sw) (* -1 (- height (* 2 sw)) (/ (first (second history)) area)))
                                                         :stroke "gold" :stroke-width sw :stroke-linecap "round"}]
                           ^{:key (swap! line-id inc)} [:line
                                                        {:x1 x
                                                         :x2 (+ x stepx)
                                                         :y1 (+ (- height sw) (* (:magnify-sharks @board) -1 (- height (* 2 sw)) (/ (second (first history)) area)))
                                                         :y2 (+ (- height sw) (* (:magnify-sharks @board) -1 (- height (* 2 sw)) (/ (second (second history)) area)))
                                                         :stroke "lightslategray" :stroke-width sw :stroke-linecap "round"}])))))))])

(defn- stats! []
  [:div.modal {:id "stats"}
   [:div.modal-content {:class (let [ratio (/ window-width window-height)]
                                 (if (> ratio 1)
                                   "modal-content-large"
                                   "modal-content-small"))}
    [:span {:class "close-button"
            :onClick #(toggle-modal "stats")} "[X]"]
    [:b [:pre "   STATS"]]
    (let [fish (count (filter #(= 'fish (:type (val %))) (:board @board)))
          sharks (count (filter #(= 'shark (:type (val %))) (:board @board)))]
      (set! history (vec (drop 1 (conj history [fish sharks]))))
      [:div
       (stats-graph)
       [:pre "fish: " [:b fish] " - sharks: " [:b sharks] " - chronon:" [:b chronon]]
       "Magnify sharks: " [:b (str (:magnify-sharks @board) "x")] [:br]
       "1x "
       [slider :magnify-sharks (:magnify-sharks @board) 1 5 20]
       " 5x" [:br]
       "Stats history window width: " [:b (:history-window @board)] " chronons"[:br]
       "100 "
       [slider :history-window (:history-window @board) 100 500 20 100]
       " 500"
       ])]])

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
            (stats!)
            [:svg.board {:width (* blocksize w) :height (* blocksize h)}
             ;; not as clean as map, but faster
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
  (clear-stats!)
  (swap! state assoc :start false) 
  (swap! board assoc :board
         ;; set all elements to nil
         (apply merge (map array-map (range area)))))

(defn- update-board! []
  (if (:start @state)
    (let [prev-board (logic/sh-fi (:board @board))]
      (swap! board assoc :board (logic/next-chronon @board))
      (set! chronon (inc chronon))
      ;; pause the game if the board is unchanged from last chronon
      (if (and (= (first prev-board) (first (logic/sh-fi (:board @board))))
               (= (last prev-board) (last (logic/sh-fi (:board @board))))
               ;; but don't pause if board is filled with sharks
               (not (= (count (first prev-board)) area)))
        (swap! state assoc :start false)))))

(defn- keydown-handler [event]
  (if (.getElementById js/document "board")
    (cond
      (= 72 event.keyCode) (toggle-modal "usage")
      (= 83 event.keyCode) (toggle-modal "stats")
      (= 32 event.keyCode) (swap! state assoc :start (not (:start @state)))
      (= 67 event.keyCode) (clear-board!)
      (= 82 event.keyCode) (randomize-board!))))

(defonce touchstart {})
(defonce swipe-threshold (/ window-width 3))
(defonce time-threshold {:min 180 :max 1000})
(def timeout-timer)

(defn- touchstart-handler [event]
  (if (.getElementById js/document "board")
    (cond
      (= 2 event.touches.length) (swap! state assoc :start (not (:start @state)))
      :else (do
              (set! touchstart {:x (-> event.changedTouches (aget 0) (aget "pageX"))
                                :y (-> event.changedTouches (aget 0) (aget "pageY"))
                                :t (.getTime (js/Date.))})
              (set! timeout-timer (js/setTimeout #(show-stats) 2000))))))

(defn- touchend-handler [event]
  (let [touchend {:x (-> event.changedTouches (aget 0) (aget "pageX"))
                  :y (-> event.changedTouches (aget 0) (aget "pageY"))
                  :t (.getTime (js/Date.))}
        xdistance (- (:x touchend) (:x touchstart))
        ydistance (- (:y touchend) (:y touchstart))
        time (- (:t touchend) (:t touchstart))]
    (js/clearTimeout timeout-timer)
    (if (and (> time (:min time-threshold)) (< time (:max time-threshold)))
      (cond
        (< xdistance (* -1 swipe-threshold)) (clear-board!)
        (> xdistance swipe-threshold) (randomize-board!)
        (< ydistance (* -1 swipe-threshold)) (toggle-modal "usage")))))

(defn create-board! []
  (if (nil? (:board @board))
    (do
      ;; a watch will take care of redraw on board change
      (add-watch board :board #(draw-board))
      (js/document.addEventListener "keydown" keydown-handler)
      (js/document.addEventListener "touchstart" touchstart-handler)
      (js/document.addEventListener "touchend" touchend-handler)
      ;; fill initial board
      (randomize-board!)
      (swap! state assoc :start true)
      ;; call update-board! every 200 ms
      (swap! state assoc :interval (js/setInterval update-board! 200))))
  state)
