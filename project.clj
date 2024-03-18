(defproject wa-tor "1.5-SNAPSHOT"
  :description "A population dynamics simulation devised by A.K. Dewdney."
  :url "http://wa-tor.saidone.org"
  :license {:name "MIT"
            :url "https://raw.githubusercontent.com/saidone75/wa-tor/master/LICENSE"}

  :min-lein-version "2.7.1"

  :plugins [[com.chartbeat.cljbeat/lein-assemble "0.1.2"]]

  :assemble {:filesets {"htdocs" [["resources/public/*"]]}
             :archive {:format :tgz :root-dir ""}}

  :dependencies [[org.clojure/clojure "1.11.2"]
                 [org.clojure/clojurescript "1.11.132"]
                 [reagent "1.2.0"]
                 [cljsjs/react "18.2.0-1"]
                 [cljsjs/react-dom "18.2.0-1"]]

  :source-paths ["src"]

  :resource-paths ["target" "resources"]

  :clean-targets ^{:protect false} ["target"]

  :aliases {"fig"       ["trampoline" "run" "-m" "figwheel.main"]
            "fig:build" ["trampoline" "run" "-m" "figwheel.main" "-b" "wa-tor" "-r"]
            "fig:min"   ["run" "-m" "figwheel.main" "-O" "advanced" "-bo" "wa-tor"]}

  :profiles {:dev {:dependencies [[com.bhauman/figwheel-main "0.2.18"]
                                  [com.bhauman/rebel-readline-cljs "0.1.4"]]}})
