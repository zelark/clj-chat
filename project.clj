(defproject clj-chat "0.1.0-SNAPSHOT"
  :description "Simple chat based on firebase and re-frame"
  :url "https://github.com/zelark/clj-chat"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  
  :min-lein-version "2.7.1"

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.238"]
                 [reagent "0.7.0"]
                 [re-frame "0.10.5"]
                 [bidi "2.1.3" :exclusions [ring/ring-core prismatic/schema]]
                 [venantius/accountant "0.2.4"]
                 [cljsjs/firebase "5.0.4-1"]]

  :plugins [[lein-figwheel "0.5.16"]
            [lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]]

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src"]
                :figwheel {:on-jsload "clj-chat.core/on-js-reload"}
                :compiler {:main clj-chat.core
                           :asset-path "/js/compiled/out"
                           :output-to "resources/public/js/compiled/clj_chat.js"
                           :output-dir "resources/public/js/compiled/out"
                           :source-map-timestamp true
                           ;; To console.log CLJS data-structures make sure you enable devtools in Chrome
                           ;; https://github.com/binaryage/cljs-devtools
                           :preloads [devtools.preload]}}

               ;; This next build is a compressed minified build for
               ;; production. You can build this with:
               ;; lein cljsbuild once min
               {:id "min"
                :source-paths ["src"]
                :compiler {:output-to "resources/public/js/compiled/clj_chat.js"
                           :main clj-chat.core
                           :optimizations :advanced
                           :pretty-print false}}]}

  :figwheel {:http-server-root "public"
             :server-port 3449
             :server-ip "127.0.0.1"
             :css-dirs ["resources/public/css"] ;; watch and update CSS
             :ring-handler server/handler
             }


  ;; Setting up nREPL for Figwheel and ClojureScript dev
  ;; Please see:
  ;; https://github.com/bhauman/lein-figwheel/wiki/Using-the-Figwheel-REPL-within-NRepl
  :profiles {:dev {:dependencies [[binaryage/devtools "0.9.9"]
                                  [figwheel-sidecar "0.5.16" :exclusions [org.clojure/tools.nrepl]]
                                  [cider/piggieback "0.3.1"]]
                   ;; need to add dev source path here to get user.clj loaded
                   :source-paths ["src" "dev"]
                   ;; for CIDER
                   ;; :plugins [[cider/cider-nrepl "0.12.0"]]
                   :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}
                   ;; need to add the compliled assets to the :clean-targets
                   :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                                     :target-path]}})
