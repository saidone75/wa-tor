(ns wa-tor.logic)

(def board (atom (array-map)))
(def state (atom {}))
(def already-moved (atom #{}))
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

;; populate board with n fishes and n sharks, randomly placed
(defn populate-board [w h nfishes nsharks shark-energy]
  (let [;; take n fishes
        fishes (set (take nfishes (shuffle (range 0 (* w h)))))
        ;; take n sharks, avoiding squares already occupied
        sharks
        (loop [candidates (shuffle (range 0 (* w h)))
               sharks #{}]
          (if (= (count sharks) nsharks)
            sharks
            (recur (rest candidates)
                   (if (contains? fishes (first candidates))
                     sharks
                     (conj sharks (first candidates))))))]
    (loop [i 0]
      (if (= i (* w h))
        @board
        (do
          (cond
            (contains? fishes i) (swap! board assoc i {:type 'fish :age 0})
            (contains? sharks i) (swap! board assoc i {:type 'shark :age 0 :energy shark-energy})
            :else (swap! board assoc i nil))
          (recur (inc i)))))))

;; retrieve sharks and fishes indices
(defn sh-fi [board]
  (loop [board board i 0 sharks '() fishes '()]
    (if (empty? board)
      [sharks fishes]
      (recur
       (rest board)
       (inc i)
       (if (= 'shark (:type (val (first board)))) (conj sharks i) sharks)
       (if (= 'fish (:type (val (first board)))) (conj fishes i) fishes)))))

;; move a single fish with index i and return an updated board
(defn- move-fish [i]
  (let [{w :w h :h fish-breed :fish-breed} @state
        fish (get @board i)
        neighbours (neighbours i w h)
        ;; random nearby free square if any
        free-square (first (shuffle (filter #(nil? (val %)) (zipmap neighbours (map #(get @board %) neighbours)))))]
    (if (not (nil? free-square))
      (if (> (:age fish) fish-breed)
        ;; reproduce and move to a nearby square
        (do
          (swap! board assoc i {:type 'fish :age 0} (first free-square) {:type 'fish :age 0})
          (swap! already-moved conj (first free-square)))
        ;; move only
        (do
          (swap! board assoc i nil (first free-square) (update fish :age inc))
          (swap! already-moved conj (first free-square))))
      ;; with no free squares around increase age only
      (swap! board assoc i (update fish :age inc)))))

;; move a single shark with index i and return an updated board
(defn- move-shark [i]
  (let [{w :w h :h shark-energy :shark-energy shark-breed :shark-breed} @state
        shark (get @board i)
        neighbours (neighbours i w h)
        ;; random nearby fish if any
        nearby-fish (first (shuffle (filter #(= 'fish (:type (val %))) (zipmap neighbours (map #(get @board %) neighbours)))))
        ;; random nearby free square if any
        free-square (first (shuffle (filter #(nil? (val %)) (zipmap neighbours (map #(get @board %) neighbours)))))]
    (if (<= (:energy shark) 0)
      ;; shark die from starvation
      (swap! board assoc i nil)
      (if (not (nil? nearby-fish))
        (if (> (:age shark) shark-breed)
          ;; reproduce and eat a nearby fish
          (do (swap! board assoc i {:type 'shark :age 0 :energy shark-energy} (first nearby-fish) {:type 'shark :age 0 :energy shark-energy})
              (swap! already-moved conj (first nearby-fish)))
          ;; eat a nearby fish
          (do (swap! board assoc i nil (first nearby-fish) (assoc shark :age (inc (:age shark)) :energy (inc (:energy shark))))
              (swap! already-moved conj (first nearby-fish))))
        (if (not (nil? free-square))
          (if (> (:age shark) shark-breed)
            ;; reproduce and move to a nearby square
            (do (swap! board assoc i {:type 'shark :age 0 :energy shark-energy} (first free-square) {:type 'shark :age 0 :energy (:energy shark)})
                (swap! already-moved conj (first free-square)))
            ;; move only
            (do (swap! board assoc i nil (first free-square) (assoc shark :age (inc (:age shark)) :energy (dec (:energy shark))))
                (swap! already-moved conj (first free-square))))
          ;; with no free squares around decrease energy only
          (swap! board assoc i (assoc shark :age (inc (:age shark)) :energy (dec (:energy shark)))))))))

(defn- move-creature [i]
  (if (nil? (get @already-moved i))
    (if (= 'shark (:type (get @board i)))
      (move-shark i)
      (move-fish i))))

(defn next-chronon [b]
  (do
    (reset! state (dissoc b :board))
    (reset! board (:board b))
    (reset! already-moved #{})
    (loop [creatures-to-move (shuffle (map key (filter #(not (nil? (val %))) @board)))]
      (if (empty? creatures-to-move)
        @board
        (do
          (move-creature (first creatures-to-move))
          (swap! already-moved conj (first creatures-to-move))
          (recur (rest creatures-to-move)))))))
