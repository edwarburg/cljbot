(ns cljbot.irc
  (:use [cljbot.messages :as msgs]))


(def blank-msg {:prefix "" :command "" :args '()})


(defn write-msg [connection msg]
  "Send msg to server given by connection."
  (doto (:out @connection)
    (.println (str (msgs/msg->str msg) "\r")) ; message gets \r\n
    (.flush))
  nil)


(defn login [connection user]
  "Log in on the connection with the supplied user."
  (write-msg connection
             (assoc blank-msg
               :command "NICK"
               :args (list (:nick user))))
  (write-msg connection
             (assoc blank-msg
               :command "USER"
               :args (list (:username user) "0" "*" ":" (:realname user)))))

(defn pong [connection msg]
  "Send a pong. Assumes msg is a PING."
  (println "Responding to " (msgs/msg->str msg))
  (write-msg connection (assoc msg
                          :command "PONG")))

(defn join [connection channel]
  "Send a JOIN message."
  (write-msg connection (assoc blank-msg
                               :command "JOIN"
                               :args (list channel))))

(defn part [connection channel]
  "Sends a PART message."
  (write-msg connection (assoc blank-msg
                          :command "PART"
                          :args (list channel))))

(defn say [connection target message]
  "Send a PRIVMSG to target, containing message. Used for both channels and users."
  (println "Sending to: " target " <> " message)
  (write-msg connection (assoc blank-msg
                          :command "PRIVMSG"
                          :args (list target
                                      (str ":" message)))))

(defn nick [connection new-nick]
  "Change the bot's nickname to new-nick."
  (write-msg connection (assoc blank-msg
                          :command "NICK"
                          :args (list new-nick))))

(defn quit [connection]
  "Quit the IRC session."
  (write-msg connection (assoc blank-msg
                          :command "QUIT")))
