(defproject wa-tor "1.4-SNAPSHOT"
  :description "A population dynamics simulation devised by A.K. Dewdney."
  :url "http://wa-tor.saidone.org"
  :license {:name "MIT"
            :url "https://raw.githubusercontent.com/saidone75/wa-tor/master/LICENSE"}

  :min-lein-version "2.7.1"

  :plugins [[com.chartbeat.cljbeat/lein-assemble "0.1.2"]]

  :assemble {:filesets {"htdocs" [["resources/public/*"]]}
             :archive {:format :tgz :root-dir ""}}

  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.clojure/clojurescript "1.10.866"]
                 [reagent "1.1.0"]
                 [cljsjs/react "17.0.2-0"]
                 [cljsjs/react-dom "17.0.2-0"]]

  :source-paths ["src"]

  :aliases {"fig"       ["trampoline" "run" "-m" "figwheel.main"]
            "fig:build" ["trampoline" "run" "-m" "figwheel.main" "-b" "wa-tor" "-r"]
            "fig:min"   ["run" "-m" "figwheel.main" "-O" "advanced" "-bo" "wa-tor"]}

  :profiles {:dev {:dependencies [[com.bhauman/figwheel-main "0.2.13"]
                                  [com.bhauman/rebel-readline-cljs "0.1.4"]]}})
