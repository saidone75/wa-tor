;; Copyright (c) 2020-2021 Saidone

(ns wa-tor.logic)

;; local board work copy
(def board (atom (array-map)))
;; plain map, set on next-chronon function
(def state {})

;;normally distributed random int
(defn- normal-random-int []
  (Math/round
   (* (Math/cos (* 2 Math/PI (rand)))
      (Math/sqrt (* -2 (Math/log (rand)))))))

;; add randomness to threshold
(defn- randomize [threshold]
  (if (:random state)
    (max 1 (+ (normal-random-int) threshold))
    threshold))

;; calculate vector of neighbours for seq index n
(defn- neighbours [n w h]
  (let [x (mod n w) y (quot n w)]
    [(+ (mod (dec x) w) (* y w))
     (+ (mod (inc x) w) (* y w))
     (+ x (* (mod (dec y) h) w))
     (+ x (* (mod (inc y) h) w))]))

;; populate board with nfish fish and nsharks sharks, randomly placed
(defn randomize-board! [state]
  (let [{w :w h :h nfish :nfish nsharks :nsharks fbreed :fbreed sbreed :sbreed starve :starve} state
        ;; take n fish
        fish (set (take nfish (shuffle (range 0 (* w h)))))
        ;; take n sharks, avoiding squares already occupied
        sharks
        (loop [candidates (shuffle (range 0 (* w h)))
               sharks #{}]
          (if (= (count sharks) nsharks)
            sharks
            (recur (rest candidates)
                   (if (contains? fish (first candidates))
                     sharks
                     (conj sharks (first candidates))))))]
    (reset! board (apply merge (map array-map (range (* w h)))))
    (run!
     ;; random age between 0 and fbreed
     #(swap! board assoc % {:type 'fish :age (rand-int (inc fbreed))})
     fish)
    (run!
     ;; random age between 0 and sbreed, random starve between 0 and starve
     #(swap! board assoc % {:type 'shark :age (rand-int (inc sbreed)) :starve (rand-int (inc starve))})
     sharks)
    @board))

;; retrieve sharks and fish indices
(defn sh-fi [board]
  [(keys (filter #(= 'shark (:type (val %))) board))
   (keys (filter #(= 'fish (:type (val %))) board))])

;; move a single fish with index i
(defn- move-fish! [i]
  (let [{w :w h :h fbreed :fbreed} state
        ;; current fish
        fish (get @board i)
        ;; neighbours of i
        neighbours (neighbours i w h)
        ;; random nearby free square if any
        free-square (ffirst (shuffle (filter #(nil? (val %)) (zipmap neighbours (map #(get @board %) neighbours)))))]
    (if-not (nil? free-square)
      (if (>= (:age fish) (randomize fbreed))
        ;; reproduce
        (swap! board assoc i {:type 'fish :age 0} free-square {:type 'fish :age 0})
        ;; move only
        (swap! board assoc i nil free-square (update fish :age inc)))
      ;; with no free squares around increase age only
      (swap! board assoc i (update fish :age inc)))))

;; move a single shark with index i
(defn- move-shark! [i]
  (let [{w :w h :h sbreed :sbreed starve :starve} state
        ;; current shark
        shark (get @board i)
        ;; neighbours of i
        neighbours (neighbours i w h)
        ;; random nearby fish if any
        nearby-fish (ffirst (shuffle (filter #(= 'fish (:type (val %))) (zipmap neighbours (map #(get @board %) neighbours)))))
        ;; random nearby free square if any
        free-square (ffirst (shuffle (filter #(nil? (val %)) (zipmap neighbours (map #(get @board %) neighbours)))))]
    (if (>= (:starve shark) (randomize starve))
      ;; shark die from starvation
      (swap! board assoc i nil)
      (if-not (nil? nearby-fish)
        (if (>= (:age shark) (randomize sbreed))
          ;; reproduce and eat a nearby fish
          (swap! board assoc i {:type 'shark :age 0 :starve 0} nearby-fish {:type 'shark :age 0 :starve 0})
          ;; eat a nearby fish
          (swap! board assoc i nil nearby-fish {:type 'shark :age (inc (:age shark)) :starve 0}))
        (if-not (nil? free-square)
          (if (>= (:age shark) (randomize sbreed))
            ;; reproduce
            (swap! board assoc i {:type 'shark :age 0 :starve (:starve shark)} free-square {:type 'shark :age 0 :starve (:starve shark)})
            ;; move only
            (swap! board assoc i nil free-square (assoc shark :age (inc (:age shark)) :starve (inc (:starve shark)))))
          ;; with no free squares around just increase age and starve
          (swap! board assoc i (assoc shark :age (inc (:age shark)) :starve (inc (:starve shark)))))))))

;; compute board for next chronon
(defn next-chronon [current-board]
  ;; state map for not carrying around too many parameters
  (set! state (dissoc current-board :current-board))
  ;; set local board work copy
  (reset! board (:current-board current-board))
  (let [[sharks fish] (map shuffle (sh-fi @board))]
    ;; move all fish
    (run!
     move-fish!
     fish)
    ;; move all sharks
    (run!
     move-shark!
     sharks))
  ;; return updated board
  @board)
