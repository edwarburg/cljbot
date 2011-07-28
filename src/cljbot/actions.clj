(ns cljbot.actions
  (:use [cljbot.messages :as msgs]
        [cljbot.irc]))

(defmulti handle-msg (fn [args] (-> args :msg :command)))

(defmethod handle-msg :default [args]
  nil)

(defmacro def-response [name args & body]
  `(defmethod handle-msg ~name ~args
     ~@body))

(def-response "PING" [args]
  (pong (:conn args) (:msg args)))
  

(def-response "JOIN" [args]
  (let [my-nick (-> args :client-info :user :nick)
        other-nick (-> args :msg :prefix msgs/extract-username)
        channel (-> args :client-info :channel)]
    (if (not= my-nick other-nick)
      (say (:conn args) channel (str "hello, " other-nick)))))


(def-response "PRIVMSG" [args]
  (let [my-nick (-> args :client-info :user :nick)
        other-nick (-> args :msg :prefix msgs/extract-username)
        target (first (-> args :msg :args))
        text (last (-> args :msg :args))
        front (apply str (next (take (inc (count my-nick)) text)))]
    (if (= front my-nick)
      (say (:conn args) target (str other-nick ":"
                                  (apply str (drop (+ 2 (count my-nick)) text)))))))


(def-response "ERROR" [args]
   (quit (:conn args)))
