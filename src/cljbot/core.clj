(ns cljbot.core
  (:use [cljbot.messages :as msgs]
        [cljbot.irc]
        [cljbot.actions]
        [cljbot.addons]
        [clojure.stacktrace]
        [clojure.contrib.duck-streams :only (writer)])
  (:import (java.net Socket
                     SocketException)
           (java.io PrintWriter
                    InputStreamReader
                    BufferedReader
                    PrintStream
                    BufferedOutputStream
                    FileOutputStream
                    InterruptedIOException)))

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
    (dosync (alter conn
                   merge
                   {:thread (Thread. #(bot-loop conn client-info))}))
    (.start (:thread @conn))
    conn))


(defn make-connection 
  "Make a connection to the server."
  [server]
  (let [socket (Socket. (:host server) (:port server))
        in (BufferedReader. (InputStreamReader. (.getInputStream socket)))
        out (PrintWriter. (.getOutputStream socket))
        conn (ref {:socket socket :in in :out out})]
    conn))


(defn clean-up [connection]
  (println ">>> Closing socket...")
  (.close (:socket @connection))
  (println ">>> Interrupting bot-loop...")
  (.interrupt (:thread @connection)))


(defn all-done [connection]
  (quit connection)
  (clean-up connection)
  (println "\n\n\n>>> Successfully quit."))


(defn handle-next-msg [connection client-info]
  (let [msg (.readLine (:in @connection))]
    (when (not (nil? msg))
      (println  msg)
      (let [args {:msg (msgs/str->msg msg)
                  :conn connection
                  :client-info client-info}]
        (cljbot.actions/handle-msg args)))))


(defn bot-loop 
  "The main bot loop: Take a command, respond to it, and log it."
  [connection client-info]
  (binding [*out* (writer ".bot.log")
            *err* *out*]
    (try
      (loop []
        (handle-next-msg connection client-info)
        (recur))
      (catch InterruptedIOException e
        (println ">>> Interrupted"))
      (catch SocketException e
        (println ">>> Failed to read:" (.getMessage e)))
      (catch Exception e
        (println ">>> ------ Exception ------")
        (print-stack-trace e)
        (println ">>> ------    end    ------"))
      (finally
       (all-done connection)))))


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
(def ci {:channel "#cljbot" :user (make-user "cljbot")})
