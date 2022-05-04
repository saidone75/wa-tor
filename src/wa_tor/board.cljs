;; Copyright (c) 2020-2022 Saidone

(ns wa-tor.board
  (:require
   [reagent.core :as reagent :refer [atom]]
   [wa-tor.logic :as logic]))

(defonce window-width  (.-innerWidth js/window))
(defonce window-height  (.-innerHeight js/window))

(defonce blocksize 12)

(defonce board (atom {}))

(defonce state (reagent/atom {}))

;; drawing context for board canvas
(def ctx nil)

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
;; extra randomness active by default
(swap! board assoc :random true)
;; trails
(swap! board assoc :trails false)

;; stats
(defonce stats (atom {}))
;; default stats window width
(swap! stats assoc :history-window 250)
;; magnify sharks stats
(swap! stats assoc :magnify-sharks 1)
;; history for stats
(defonce history-size 400)
;; history buffer
(defonce history (vec (take history-size (repeat []))))
;; unique id for lines
(defonce line-id (atom 0))
;; chronon counter
(defonce chronon 0)

(defn clear-stats! []
  (set! history (vec (take history-size (repeat []))))
  (set! chronon 0))

(defn- randomize-board! []
  (clear-stats!)
  (swap! board assoc :current-board (logic/randomize-board! (dissoc @board :current-board)))
  ;; extra board for trails
  (swap! board assoc :prev-board (:current-board @board)))

(defn- toggle-modal [id]
  (-> (.getElementById js/document id) (aget "classList") (.toggle "show-modal")))

(defn- show-stats []
  (when-not (= "modal show-modal" (-> (.getElementById js/document "usage") (aget "classList") (aget "value")))
    (-> (.getElementById js/document "stats") (aget "classList") (.add "show-modal"))))

(defn- toggle [e]
  (if (:start @state)
    (toggle-modal "usage")
    (let [x (quot (-> e (aget "nativeEvent") (aget "offsetX")) blocksize)
          y (quot (-> e (aget "nativeEvent") (aget "offsetY")) blocksize)
          id (+ x (* y (:w @board)))
          type (:type (get (:current-board @board) id))]
      (cond
        (= type 'fish) (swap! board assoc :current-board (assoc (:current-board @board) id {:type 'shark :age 0 :starve 0}))
        (= type 'shark) (swap! board assoc :current-board (assoc (:current-board @board) id nil))
        :else (swap! board assoc :current-board (assoc (:current-board @board) id {:type 'fish :age 0}))))))

(defn slider [reference key value min max width step]
  [:input {:type "range" :value value :min min :max max
           :style {:width (if-not (nil? width) (str width "%") "80%")}
           :step (str step)
           :onChange (fn [e]
                       (let [new-value (js/parseInt (.. e -target -value))]
                         (swap! reference assoc key new-value)))}])

(defn checkbox [reference key]
  [:input {:type :checkbox :checked (key @reference)
           :onChange (fn []
                       (swap! reference assoc key (not (key @reference))))}])

(defn splash []
  [:div.modal {:id "splash" }
   [:div.modal-content {:class (let [ratio (/ window-width window-height)]
                                 (if (> ratio 1)
                                   "splash-content-large"
                                   "splash-content-small"))}
    [:span {:class "close-button"
            :onClick #(toggle-modal "splash")} "[X]"]
    [:h1 "Wa-Tor"]
    [:h3 "A population dynamics simulation devised by A.K. Dewdney"]
    [:h4 "Press P or swipe up to show the control panel"]
    "Implemented in ClojureScript/ReactJS" [:br] [:br]
    "Based on "[:a {:href "https://github.com/saidone75/wa-tor/blob/master/wator_dewdney.pdf"} "the original article"] " appeared on the December 1984 issue of Scientific American" [:br] [:br]
    "You can grab the source code and leave feedback " [:a {:href "https://github.com/saidone75/wa-tor"} "here"] [:br] [:br]
    "Distributed under the " [:a {:href "https://github.com/saidone75/wa-tor/blob/master/LICENSE"} "MIT License"] [:br] [:br]
    "Copyright (c) 2020-2022 " [:a {:href "https://saidone.org"} "Saidone"]]])

(defn usage-panel []
  [:div.modal {:id "usage"}
   [:div.modal-content {:class (let [ratio (/ window-width window-height)]
                                 (if (> ratio 1)
                                   "modal-content-large"
                                   "modal-content-small"))}
    [:span {:class "close-button"
            :onClick #(toggle-modal "usage")} "[X]"]
    [:b [:pre "   USAGE"]]
    [:table {:class "usage"}
     [:tbody
      [:tr [:th "COMMAND"] [:th "KEYBOARD"] [:th "TOUCHSCREEN"]] 
      [:tr [:td "pause/resume"] [:td "spacebar"] [:td "two fingers tap"]]
      [:tr [:td "clear board"] [:td "C"] [:td "swipe left"]]
      [:tr [:td "randomize board"] [:td "R"] [:td "swipe right"]]
      [:tr [:td "toggle usage panel"] [:td "P"] [:td "swipe up"]]
      [:tr [:td "show stats"] [:td "S"] [:td "long touch (> 2s)"]]]
     ]
    [:br]
    "when paused click/tap a square to cycle between" [:br]
    "water >>> fish >>> shark" [:br] [:br]
    [:div
     "Initial number of fish: " [:b (:nfish @board)] [:br]
     [slider board :nfish (:nfish @board) 0 (- area (:nsharks @board))]]
    [:div
     "Initial number of sharks: " [:b (:nsharks @board)] [:br]
     [slider board :nsharks (:nsharks @board) 0 (- area (:nfish @board))]]
    [:div
     "Fish breed time: " [:b (:fbreed @board)] " chronons" [:br]
     [slider board :fbreed (:fbreed @board) 1 20]]
    [:div
     "Shark breed time: " [:b (:sbreed @board)] " chronons" [:br]
     [slider board :sbreed (:sbreed @board) 1 20]]
    [:div
     "Shark starve after: " [:b (:starve @board)] " chronons w/o food" [:br]
     [slider board :starve (:starve @board) 1 20]]
    [:table {:class "switches"}
     [:tbody
      [:tr
       [:td {:class "switches"}
        "Extra randomness: " [:b (if (:random @board) "on" "off")] [:br]
        "off"
        [:label {:class "switch"}
         [checkbox board :random]
         [:span {:class "slider"}]]
        "on"
        [:br]]
       [:td {:class "switches"}
        "Show trails: " [:b (if (:trails @board) "on" "off")] [:br]
        "off"
        [:label {:class "switch"}
         [checkbox board :trails]
         [:span {:class "slider"}]]
        "on"
        [:br]]]]]]])

(defn- stats-graph []
  [:svg.stats {:id "svg.stats" :width "100%" :height "100%"}
   (when (and (= "complete" (aget js/document "readyState"))
              (not (= "modal" (aget (.getElementById js/document "stats") "classList"))))
     (let [height (aget (.getElementById js/document "svg.stats") "clientHeight")
           width (aget (.getElementById js/document "svg.stats") "clientWidth")
           stepx (/ width (:history-window @stats))
           sw 3]
       (reset! line-id 0)
       (loop [history (take-last (:history-window @stats) history) x 0 lines '()]
         (if (> 2 (count history)) lines
             (recur (drop 1 history) (+ x stepx)
                    (conj lines
                          ^{:key (swap! line-id inc)}
                          [:line
                           {:x1 (Math/round x)
                            :x2 (Math/round (+ x stepx))
                            :y1 (Math/round (+ (- height sw) (* -1 (- height (* 2 sw)) (/ (first (first history)) area))))
                            :y2 (Math/round (+ (- height sw) (* -1 (- height (* 2 sw)) (/ (first (second history)) area))))
                            :stroke "gold" :stroke-width sw :stroke-linecap "round"}]
                          ^{:key (swap! line-id inc)}
                          [:line
                           {:x1 (Math/round x)
                            :x2 (Math/round (+ x stepx))
                            :y1 (Math/round (+ (- height sw) (* (:magnify-sharks @stats) -1 (- height (* 2 sw)) (/ (second (first history)) area))))
                            :y2 (Math/round (+ (- height sw) (* (:magnify-sharks @stats) -1 (- height (* 2 sw)) (/ (second (second history)) area))))
                            :stroke "lightslategray" :stroke-width sw :stroke-linecap "round"}]))))))])

(defn stats-panel []
  [:div.modal {:id "stats"}
   [:div.modal-content {:class (let [ratio (/ window-width window-height)]
                                 (if (< 1 ratio)
                                   "modal-content-large"
                                   "modal-content-small"))}
    [:span {:class "close-button"
            :onClick #(toggle-modal "stats")} "[X]"]
    [:b [:pre "   STATS"]]
    (let [[sharks fish] (map count (logic/sh-fi (:current-board @board)))]
      (set! history (vec (drop 1 (conj history [fish sharks]))))
      [:div
       (stats-graph)
       [:pre "fish: " [:b fish] " - sharks: " [:b sharks] " - chronon:" [:b chronon]]
       "Magnify sharks: " [:b (str (:magnify-sharks @stats) "x")] [:br]
       "1x "
       [slider stats :magnify-sharks (:magnify-sharks @stats) 1 5 20]
       " 5x" [:br]
       "Stats history window width: " [:b (:history-window @stats)] " chronons"[:br]
       "100 "
       [slider stats :history-window (:history-window @stats) 100 400 20 50]
       " 400"])]])

(defn- block [x y color]
  (set! (.-fillStyle ctx) color)
  (.fillRect ctx (inc x) (inc y) (- blocksize 2) (- blocksize 2)))

(defn draw-board []
  (let [{w :w h :h} @board]
    [:div {:id "sea" :class "sea" :width (* blocksize w) :height (* blocksize h)}
     [:canvas {:id "canvas" :width (* blocksize w) :height (* blocksize h) :onClick (fn [e] (toggle e))}]]))

(defn- redraw-board []
  (let [{w :w current-board :current-board prev-board :prev-board} @board]
    (.clearRect ctx 0 0 (* (:w @board) blocksize) (* (:h @board) blocksize))
    (run!
     #(if-not (nil? (:type (val %)))
        (block
         (* blocksize (mod (key %) w))
         (* blocksize (quot (key %) w))
         (cond
           (= 'fish (:type (val %))) "gold"
           (= 'shark (:type (val %))) "lightslategray"))
        ;; if on sea then check previous board for trails
        (if (and (:trails @board) (not (nil? (:type (get prev-board (key %))))))
          (block
           (* blocksize (mod (key %) w))
           (* blocksize (quot (key %) w))
           (cond
             (= 'fish (:type (get prev-board (key %)))) "#99ff99"
             (= 'shark (:type (get prev-board (key %)))) "#33cccc"))))
     current-board)))

(defn- clear-board! []
  (clear-stats!)
  (swap! state assoc :start false) 
  (swap! board assoc :current-board
         ;; set all elements to nil
         (apply merge (map array-map (range area))))
  (swap! board assoc :prev-board
         ;; set all elements to nil
         (apply merge (map array-map (range area)))))

(defn- update-board! []
  (when (:start @state)
    (let [[prev-sharks prev-fish] (logic/sh-fi (:current-board @board))
          ;; actual update happens here
          [sharks fish] (logic/sh-fi (:current-board (do
                                                       (swap! board assoc :prev-board (:current-board @board))
                                                       (swap! board assoc :current-board (logic/next-chronon @board)))))]
      (set! chronon (inc chronon))
      ;; pause the game if the board is unchanged from last chronon
      (when (and (= sharks prev-sharks)
                 (= fish prev-fish)
                 ;; but don't pause if board is filled with sharks
                 (not (= (count sharks) area)))
        (swap! state assoc :start false)))))

(defn- keydown-handler [event]
  (let [key-code (aget event "keyCode")]
    (cond
      (= 32 key-code) (swap! state assoc :start (not (:start @state)))
      (= 65 key-code) (toggle-modal "splash")
      (= 67 key-code) (clear-board!)
      (= 80 key-code) (toggle-modal "usage")
      (= 82 key-code) (randomize-board!)
      (= 83 key-code) (toggle-modal "stats"))))

(defonce touchstart {})
(defonce swipe-threshold (/ window-width 3))
(defonce time-threshold {:min 180 :max 1000})
(defonce timeout-timer nil)

(defn- touchstart-handler [event]
  (cond
    (= 2 (-> event (aget "touches") (aget "length"))) (swap! state assoc :start (not (:start @state)))
    :else (do
            (set! touchstart {:x (-> event (aget "changedTouches") (aget 0) (aget "pageX"))
                              :y (-> event (aget "changedTouches") (aget 0) (aget "pageY"))
                              :t (.getTime (js/Date.))})
            (set! timeout-timer (js/setTimeout #(show-stats) 2000)))))

(defn- touchend-handler [event]
  (let [touchend {:x (-> event (aget "changedTouches") (aget 0) (aget "pageX"))
                  :y (-> event (aget "changedTouches") (aget 0) (aget "pageY"))
                  :t (.getTime (js/Date.))}
        xdistance (- (:x touchend) (:x touchstart))
        ydistance (- (:y touchend) (:y touchstart))
        time (- (:t touchend) (:t touchstart))]
    (js/clearTimeout timeout-timer)
    (when (and (> time (:min time-threshold)) (< time (:max time-threshold)))
      (cond
        (< xdistance (* -1 swipe-threshold)) (clear-board!)
        (> xdistance swipe-threshold) (randomize-board!)
        (< ydistance (* -1 swipe-threshold)) (toggle-modal "usage")))))

(defn- dom-content-loaded []
  ;; set context for board canvas
  (set! ctx (.getContext (.getElementById js/document "canvas") "2d"))
  ;; start simulation
  (swap! state assoc :start true)
  ;; a watch will take care of redraw on board change
  (add-watch board :board #(redraw-board))
  ;; call update-board! every 125 ms
  (swap! state assoc :interval (js/setInterval update-board! 125))
  ;; show control panel
  (toggle-modal "splash"))

(defn create-board! []
  (when (nil? (:board @board))
    (-> js/document (.addEventListener "keydown" keydown-handler))
    (-> js/document (.addEventListener "touchstart" touchstart-handler))
    (-> js/document (.addEventListener "touchend" touchend-handler))
    (-> js/document (.addEventListener "DOMContentLoaded" dom-content-loaded))
    ;; fill initial board
    (randomize-board!))
  state)
