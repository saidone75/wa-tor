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

;; populate board with n fishes and n sharks, randomly placed
(defn populate-board [w h nfishes fish-breed nsharks shark-breed shark-starve]
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
            (contains? fishes i) (swap! board assoc i {:type 'fish :breed (rand-int (inc fish-breed))})
            (contains? sharks i) (swap! board assoc i {:type 'shark :breed (rand-int (inc shark-breed)) :starve (rand-int (inc shark-starve))})
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

;; move a single fish with index i
(defn- move-fish [i]
  (let [{w :w h :h fish-breed :fish-breed} @state
        fish (get @board i)
        neighbours (neighbours i w h)
        ;; random nearby free square if any
        free-square (first (shuffle (filter #(nil? (val %)) (zipmap neighbours (map #(get @board %) neighbours)))))]
    (if (not (nil? free-square))
      (if (>= (:breed fish) fish-breed)
        ;; reproduce
        (swap! board assoc i {:type 'fish :breed 0} (first free-square) {:type 'fish :breed 0})
        ;; move only
        (swap! board assoc i nil (first free-square) (update fish :breed inc)))
      ;; with no free squares around increase breed only
      (swap! board assoc i (update fish :breed inc)))))

;; move a single shark with index i
(defn- move-shark [i]
  (let [{w :w h :h shark-breed :shark-breed shark-starve :shark-starve} @state
        shark (get @board i)
        neighbours (neighbours i w h)
        ;; random nearby fish if any
        nearby-fish (first (shuffle (filter #(= 'fish (:type (val %))) (zipmap neighbours (map #(get @board %) neighbours)))))
        ;; random nearby free square if any
        free-square (first (shuffle (filter #(nil? (val %)) (zipmap neighbours (map #(get @board %) neighbours)))))]
    (if (>= (:starve shark) shark-starve)
      ;; shark die from starvation
      (swap! board assoc i nil)
      (if (not (nil? nearby-fish))
        (if (>= (:breed shark) shark-breed)
          ;; reproduce and eat a nearby fish
          (swap! board assoc i {:type 'shark :breed 0 :starve 0} (first nearby-fish) {:type 'shark :breed 0 :starve 0})
          ;; eat a nearby fish
          (swap! board assoc i nil (first nearby-fish) {:type 'shark :breed (inc (:breed shark)) :starve 0}))
        (if (not (nil? free-square))
          (if (>= (:breed shark) shark-breed)
            ;; reproduce
            (swap! board assoc i {:type 'shark :breed 0 :starve (:starve shark)} (first free-square) {:type 'shark :breed 0 :starve (:starve shark)})
            ;; move only
            (swap! board assoc i nil (first free-square) (assoc shark :breed (inc (:breed shark)) :starve (inc (:starve shark)))))
          ;; with no free squares around just increase breed and starve
          (swap! board assoc i (assoc shark :breed (inc (:breed shark)) :starve (inc (:starve shark)))))))))

;; compute board for next chronon
(defn next-chronon [b]
  (do
    (reset! state (dissoc b :board))
    (reset! board (:board b))
    ;; move all fishes first 
    (run!
     move-fish
     (shuffle (map key (filter #(= 'fish (:type (val %))) @board))))
    ;; move all sharks
    (run!
     move-shark
     (shuffle (map key (filter #(= 'shark (:type (val %))) @board))))
    @board))
