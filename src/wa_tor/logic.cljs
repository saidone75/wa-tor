(ns wa-tor.logic)

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

(defn populate-board [board nfishes nsharks]
  (let [{w :w h :h shark-nrg :shark-nrg} board
        fishes (set (take nfishes (shuffle (range 0 (* w h)))))
        sharks
        (loop [candidates (shuffle (range 0 (* w h)))
               sharks #{}]
          (if (= (count sharks) nsharks)
            sharks
            (recur (rest candidates)
                   (if (contains? fishes (first candidates))
                     sharks
                     (conj sharks (first candidates))))))]
    (vec
     (for [x (range 0 (* w h))]
       (cond
         (contains? fishes x) {:type 'fish :age 0}
         (contains? sharks x) {:type 'shark :age 0 :energy shark-nrg}
         :else nil)))))

;; retrieve sharks and fishes indices
(defn sh-fi [board]
  (loop [board board i 0 sharks '() fishes '()]
    (if (empty? board)
      [sharks fishes]
      (recur
       (rest board)
       (inc i)
       (if (= 'shark (:type (first board))) (conj sharks i) sharks)
       (if (= 'fish (:type (first board))) (conj fishes i) fishes)))))

(defn- move-fish [board i]
  (assoc board :board
         (let [{board :board w :w h :h fish-preg :fish-preg} board
               fish (nth board i)
               neighbours (neighbours i w h)
               free-square (first (shuffle (filter #(nil? (val %)) (zipmap neighbours (map #(get board %) neighbours)))))]
           (if (not (nil? free-square))
             (if (> (:age fish) fish-preg)
               (assoc board i {:type 'fish :age 0} (first free-square) {:type 'fish :age 0})
               (assoc board i nil (first free-square) (update fish :age inc)))
             (assoc board i (update fish :age inc))))))

(defn- move-shark [board i]
  (assoc board :board
         (let [{board :board w :w h :h shark-nrg :shark-nrg shark-preg :shark-preg} board
               shark (nth board i)
               neighbours (neighbours i w h)
               nearby-fish (first (shuffle (filter #(= 'fish (:type (val %))) (zipmap neighbours (map #(get board %) neighbours))))) 
               free-square (first (shuffle (filter #(nil? (val %)) (zipmap neighbours (map #(get board %) neighbours)))))]
           (if (<= (:energy shark) 0)
             (assoc board i nil)
             (if (not (nil? nearby-fish))
               (if (> (:age shark) shark-preg)
                 (assoc board i {:type 'shark :age 0 :energy shark-nrg} (first nearby-fish) (assoc shark :age (inc (:age shark)) :energy (inc (:energy shark))))
                 (assoc board i nil (first nearby-fish) (assoc shark :age (inc (:age shark)) :energy (inc (:energy shark)))))
               (if (not (nil? free-square))
                 (if (> (:age shark) shark-preg)
                   (assoc board i {:type 'shark :age 0 :energy shark-nrg} (first free-square) {:type 'shark :age 0 :energy (:energy shark)})
                   (assoc board i nil (first free-square) (assoc shark :age (inc (:age shark)) :energy (dec (:energy shark)))))
                 (assoc board i (assoc shark :age (inc (:age shark)) :energy (dec (:energy shark))))))))))

(defn next-chronon [board]
  (let [sh-fi (sh-fi (:board board))
        sharks (shuffle (first sh-fi))
        fishes (shuffle (last sh-fi))]
    (:board
     (reduce
      move-shark
      (reduce
       move-fish
       board
       fishes)
      sharks))))
