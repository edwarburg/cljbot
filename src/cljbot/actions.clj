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
        channel (apply str (next (last (-> args :msg :args))))]
    (if (not= my-nick other-nick)
      (say (:conn args) channel (str "hello, " other-nick)))))


(def-response "PRIVMSG" [args]
  (let [my-nick (-> args :client-info :user :nick)
        other-nick (-> args :msg :prefix msgs/extract-username)
        pm-source (first (-> args :msg :args))
        text (last (-> args :msg :args))
        front (apply str (next (take (inc (count my-nick)) text)))
        mimic-text (apply str (drop (+ 2 (count my-nick)) text))]
    (condp = my-nick
      ; this is a /msg
      pm-source (do
                  (println "PM from:" other-nick)
                  (when (not= other-nick my-nick) ; refuse to get in PM war with yourself
                    (say (:conn args)
                         other-nick
                         (apply str (next text)))))
      ; we've been pinged (ie, someone says 'cljbot: foo')
      front (say (:conn args)
                 pm-source
                 (str other-nick ":" mimic-text))
      nil)))


(def-response "ERROR" [args]
   (quit (:conn args)))
