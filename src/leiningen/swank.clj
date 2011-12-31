(ns leiningen.swank
  "Launch swank server for Emacs to connect."
  (:use [leiningen.compile :only [eval-in-project]])
  (:import [java.io File]))

(defn swank-form [project port host opts]
  ;; bootclasspath workaround: http://dev.clojure.org/jira/browse/CLJ-673
  (when (:eval-in-leiningen project)
    (require '[clojure walk template stacktrace]))
  `(do
     (when-let [is# ~(:repl-init-script project)]
       (when (.exists (File. (str is#)))
         (load-file is#)))
     (when-let [repl-init# '~(:repl-init project)]
       (doto repl-init# require in-ns))
     (require '~'swank.swank)
     (require '~'swank.commands.basic)
     (@(ns-resolve '~'swank.swank '~'start-server)
      ~@(concat (map read-string opts)
                [:host host :port (Integer. port)
                 :repl-out-root true :block true]))))

(defn- add-jdk-toolsjar-to-classpath
  "CDT requires the JDK's tools.jar and sa-jdi.jar. Add them to the classpath."
  [project]
  (let [libdir (File. (File. (System/getProperty "java.home") "..") "lib")
        f-exists? (fn [f] (.exists f))
        extra-cp (filter f-exists? (map #(File. libdir %)
                                        ["tools.jar" "sa-jdi.jar"]))
        cp-key :extra-classpath-dirs]
    (if (seq extra-cp)
      (assoc project cp-key (apply conj (cp-key project) extra-cp))
      project)))

(defn swank
  "Launch swank server for Emacs to connect. Optionally takes PORT and HOST."
  ([project port host & opts]
     (eval-in-project
      (add-jdk-toolsjar-to-classpath project)
      (swank-form project port host opts)))
  ([project port] (swank project port "localhost"))
  ([project] (swank project 4005)))
