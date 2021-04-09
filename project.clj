(defproject wa-tor "1.3"
  :description "A population dynamics simulation devised by A.K. Dewdney."
  :url "http://wa-tor.saidone.org"
  :license {:name "MIT"
            :url "https://raw.githubusercontent.com/saidone75/wa-tor/master/LICENSE"}

  :min-lein-version "2.7.1"

  :plugins [[com.chartbeat.cljbeat/lein-assemble "0.1.2"]]

  :assemble {:filesets {"htdocs" [["resources/public/*"]]}
             :archive {:format :tgz :root-dir ""}}

  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.clojure/clojurescript "1.10.844"]
                 [reagent "1.0.0"]]

  :source-paths ["src"]

  :aliases {"fig"       ["trampoline" "run" "-m" "figwheel.main"]
            "fig:build" ["trampoline" "run" "-m" "figwheel.main" "-b" "wa-tor" "-r"]
            "fig:min"   ["run" "-m" "figwheel.main" "-O" "advanced" "-bo" "wa-tor"]}

  :profiles {:dev {:dependencies [[com.bhauman/figwheel-main "0.2.12"]
                                  [com.bhauman/rebel-readline-cljs "0.1.4"]]}})
