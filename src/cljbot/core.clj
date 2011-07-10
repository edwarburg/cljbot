(ns cljbot.core
  (:use [cljbot.messages :as msgs]
        [cljbot.irc]
        [cljbot.actions]
        [cljbot.addons]
        [clojure.stacktrace]
        [clojure.contrib.duck-streams :only (writer)])
  (:import (java.net Socket)
           (java.io PrintWriter
                    InputStreamReader
                    BufferedReader
                    PrintStream
                    BufferedOutputStream
                    FileOutputStream)))

(declare make-connection)
(declare bot-loop)

(defn connect 
  "Connect to a server, log in, and join a channel."
  [server client-info]
  (let [conn (make-connection server)
        user (:user client-info)
        channel (:channel client-info)]
    (login conn user)
    (join conn channel)
    (doto (Thread. #(bot-loop conn client-info)) (.start))
    conn))


(defn make-connection 
  "Make a connection to the server."
  [server]
  (let [socket (Socket. (:host server) (:port server))
        in (BufferedReader. (InputStreamReader. (.getInputStream socket)))
        out (PrintWriter. (.getOutputStream socket))
        conn (ref {:socket socket :in in :out out})]
    conn))


(defn bot-loop 
  "The main bot loop: Take a command, respond to it, and log it."
  [connection client-info]
  (binding [*out* (writer ".bot.log")
            *err* *out*]
    (try
      (loop []
        (let [msg (.readLine (:in @connection))]
          (when (not (nil? msg))
            (println  msg)
            (on-msg-dispatch (msgs/str->msg msg)
                             (list connection (msgs/str->msg msg) client-info))
            (recur))))
      (catch Exception e
        (println "------ Exception ------")
        (print-stack-trace e)
        (println "------    end    ------"))
      (finally
       (quit connection)))))


(defn make-server [host port]
  {:host host :port port})

(defn make-user
  ([nick]
     (make-user nick nick nick))
  ([nick user]
     (make-user nick user user))
  ([nick user real]
     {:nick nick :username user :realname real}))

; Examples, in normal usage you'd def these at the REPL and call
; (def connection (connect some-server some-client-info))
(def freenode (make-server "irc.freenode.net" 6667))
(def ci {:channel "#ufpc" :user (make-user "cljbot")})
