(ns cljbot.irc
  (:use [cljbot.messages :as msgs]))


(def blank-msg {:prefix "" :command "" :args '()})


(defn write-msg 
  "Send msg to server given by connection."
  [connection msg]
  (doto (:out @connection)
    (.println (str (msgs/msg->str msg) "\r")) ; message gets \r\n
    (.flush))
  nil)


(defn login 
  "Log in on the connection with the supplied user."
  [connection user]
  (write-msg connection
             (assoc blank-msg
               :command "NICK"
               :args (list (:nick user))))
  (write-msg connection
             (assoc blank-msg
               :command "USER"
               :args (list (:username user) "0" "*" ":" (:realname user)))))

(defn pong 
  "Send a pong. Assumes msg is a PING."
  [connection msg]
  (println "Responding to " (msgs/msg->str msg))
  (write-msg connection (assoc msg
                          :command "PONG")))

(defn join 
  "Send a JOIN message."
  [connection channel]
  (write-msg connection (assoc blank-msg
                               :command "JOIN"
                               :args (list channel))))

(defn part 
  "Sends a PART message."
  [connection channel]
  (write-msg connection (assoc blank-msg
                          :command "PART"
                          :args (list channel))))

(defn say 
  "Send a PRIVMSG to target, containing message. Used for both channels and users."
  [connection target message]
  (println "Sending to: " target " <> " message)
  (write-msg connection (assoc blank-msg
                          :command "PRIVMSG"
                          :args (list target
                                      (str ":" message)))))

(defn nick 
  "Change the bot's nickname to new-nick."
  [connection new-nick]
  (write-msg connection (assoc blank-msg
                          :command "NICK"
                          :args (list new-nick))))

(defn quit 
  "Quit the IRC session."
  [connection]
  (write-msg connection (assoc blank-msg
                          :command "QUIT")))

