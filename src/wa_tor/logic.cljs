(ns wa-tor.logic)

(def board (atom (array-map)))
(def state (atom {}))

;; calculate vector index from coords
(defn- compute-index [x y w]
  (+ x (* y w)))

;; calculate grid coords from vector index
(defn- compute-coords [n w]
  {:x (mod n w) :y (quot n w)})

;; calculate vector of neighbours for seq index n
(defn- neighbours [n w h]
  (let [coords (compute-coords n w)
        ;; inc x and y with wrapping logic
        incx #(cond
                (= %1 (dec w)) 0
                :else (inc %1))
        decx #(cond
                (= %1 0) (dec w)
                :else (dec %1))
        incy #(cond
                (= %1 (dec h)) 0
                :else (inc %1))
        decy #(cond
                (= %1 0) (dec h)
                :else (dec %1))]
    [(compute-index (decx (:x coords)) (:y coords) w)
     (compute-index (incx (:x coords)) (:y coords) w)
     (compute-index (:x coords) (decy (:y coords)) w)
     (compute-index (:x coords) (incy (:y coords)) w)]))

;; populate board with nfish fish and nsharks sharks, randomly placed
(defn populate-board! [state]
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
    (loop [i 0]
      (if (= i (* w h))
        @board
        (do
          (cond
            ;; random age between 0 and fbreed
            (contains? fish i) (swap! board assoc i {:type 'fish :age (rand-int (inc fbreed))})
            ;; random age between 0 and sbreed, random starve between 0 and starve
            (contains? sharks i) (swap! board assoc i {:type 'shark :age (rand-int (inc sbreed)) :starve (rand-int (inc starve))})
            :else (swap! board assoc i nil))
          (recur (inc i)))))))

;; retrieve sharks and fish indices
(defn sh-fi [board]
  [(map key (filter #(= 'shark (:type (val %))) board))
   (map key (filter #(= 'fish (:type (val %))) board))])

;; move a single fish with index i
(defn- move-fish! [i]
  (let [{w :w h :h fbreed :fbreed} @state
        fish (get @board i)
        neighbours (neighbours i w h)
        ;; random nearby free square if any
        free-square (first (shuffle (filter #(nil? (val %)) (zipmap neighbours (map #(get @board %) neighbours)))))]
    (if (not (nil? free-square))
      (if (>= (:age fish) fbreed)
        ;; reproduce
        (swap! board assoc i {:type 'fish :age 0} (first free-square) {:type 'fish :age 0})
        ;; move only
        (swap! board assoc i nil (first free-square) (update fish :age inc)))
      ;; with no free squares around increase age only
      (swap! board assoc i (update fish :age inc)))))

;; move a single shark with index i
(defn- move-shark! [i]
  (let [{w :w h :h sbreed :sbreed starve :starve} @state
        shark (get @board i)
        neighbours (neighbours i w h)
        ;; random nearby fish if any
        nearby-fish (first (shuffle (filter #(= 'fish (:type (val %))) (zipmap neighbours (map #(get @board %) neighbours)))))
        ;; random nearby free square if any
        free-square (first (shuffle (filter #(nil? (val %)) (zipmap neighbours (map #(get @board %) neighbours)))))]
    (if (>= (:starve shark) starve)
      ;; shark die from starvation
      (swap! board assoc i nil)
      (if (not (nil? nearby-fish))
        (if (>= (:age shark) sbreed)
          ;; reproduce and eat a nearby fish
          (swap! board assoc i {:type 'shark :age 0 :starve 0} (first nearby-fish) {:type 'shark :age 0 :starve 0})
          ;; eat a nearby fish
          (swap! board assoc i nil (first nearby-fish) {:type 'shark :age (inc (:age shark)) :starve 0}))
        (if (not (nil? free-square))
          (if (>= (:age shark) sbreed)
            ;; reproduce
            (swap! board assoc i {:type 'shark :age 0 :starve (:starve shark)} (first free-square) {:type 'shark :age 0 :starve (:starve shark)})
            ;; move only
            (swap! board assoc i nil (first free-square) (assoc shark :age (inc (:age shark)) :starve (inc (:starve shark)))))
          ;; with no free squares around just increase age and starve
          (swap! board assoc i (assoc shark :age (inc (:age shark)) :starve (inc (:starve shark)))))))))

;; compute board for next chronon
(defn next-chronon [current-board]
  (do
    (reset! state (dissoc current-board :board))
    (reset! board (:board current-board))
    ;; move all fish first 
    (run!
     move-fish!
     ;; all fish, randomly picked
     (shuffle (map key (filter #(= 'fish (:type (val %))) @board))))
    ;; move all sharks
    (run!
     move-shark!
     ;; all sharks, randomly picked
     (shuffle (map key (filter #(= 'shark (:type (val %))) @board))))
    @board))
