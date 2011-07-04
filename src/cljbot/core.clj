(ns cljbot.core
  (:use [cljbot.messages :as msgs]
        [cljbot.irc]
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
(declare on-msg-dispatch)

(defn connect [server client-info]
  "Connect to a server, log in, and join a channel."
  (let [conn (make-connection server)
        user (:user client-info)
        channel (:channel client-info)]
    (login conn user)
    (join conn channel)
    (doto (Thread. #(bot-loop conn client-info)) (.start))
    conn))


(defn make-connection [server]
  "Make a connection to the server."
  (let [socket (Socket. (:host server) (:port server))
        in (BufferedReader. (InputStreamReader. (.getInputStream socket)))
        out (PrintWriter. (.getOutputStream socket))
        conn (ref {:socket socket :in in :out out})]
    conn))


(defn bot-loop [connection client-info]
  "The main bot loop: Take a command, respond to it, and log it."
  (binding [*out* (writer ".bot.log")]
    (let [channel (:channel client-info)
          nick (-> client-info :user :nick)]
      (loop []
        (let [msg (.readLine (:in @connection))]
          (when (not (nil? msg))
            (println  msg)
            (on-msg-dispatch (msgs/str->msg msg)
                             (list connection (msgs/str->msg msg) client-info))
            (recur)))))))


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
(def ci {:channel #"#ufpc" :user (make-user "cljbot")})



(defmacro def-actions [dispatch-name & callbacks]
  (let [args-gensym `args#
        msg-names (map first callbacks)
        fn-bodies (map second callbacks)
        fn-names (map #(symbol (str "on-" (clojure.contrib.string/lower-case %))) msg-names)]
    `(do
       ~@(for [i (range (count fn-names))]
                         `(defn ~(nth fn-names i) ~'[connection msg client-info]
                            ~(nth fn-bodies i)))
       (defn ~dispatch-name [msg# ~args-gensym]
         (condp = (:command msg#)
           ~@(apply concat (for [i (range (count msg-names))]
                             `(~(nth msg-names i) (apply ~(nth fn-names i) ~args-gensym))))
           nil)))))


; The meat of the bot: Each pair ["COMMAND" (..)] declares a function
; to handle the type of message given by "COMMAND". When a COMMAND message is received,
; the corresponding body will be executed.
;
; These arguments are implicitly passed: [connection msg client-info]
; which are exactly what they sound like.
(def-actions on-msg-dispatch
  ["PING"
   (pong connection msg)]
  
  ["JOIN"
   (let [my-nick (-> client-info :user :nick)
         other-nick (-> msg :prefix msgs/extract-username)
         channel (:channel client-info)]
     (if (not= my-nick other-nick)
       (say connection channel (str "hello, " other-nick))))]

  ["PRIVMSG"
   (let [my-nick (-> client-info :user :nick)
         other-nick (-> msg :prefix msgs/extract-username)
         target (first (:args msg))
         text (last (:args msg))
         front (apply str (next (take (inc (count my-nick)) text)))]
     (if (= front my-nick)
       (say connection target (str other-nick ":"
                                    (apply str (drop (+ 2 (count my-nick)) text))))))]

  ["ERROR"
   (quit connection)])
