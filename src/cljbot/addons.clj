(ns cljbot.addons
  (:use [cljbot.actions]
        [cljbot.messages :as msgs]
        [cljbot.irc]))

(def-response PRIVMSG
  (let [my-nick (-> client-info :user :nick)
        other-nick (-> msg :prefix msgs/extract-username)
        target (first (:args msg))
        text (last (:args msg))
        front (apply str (next (take (inc (count my-nick)) text)))]
    (if (= front my-nick)
      (say connection target (str other-nick ":"
                                  (apply str (drop (+ 2 (count my-nick)) text)))))))
