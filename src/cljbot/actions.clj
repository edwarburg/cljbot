(ns cljbot.actions
  (:use [cljbot.messages :as msgs]
        [cljbot.irc]))

(def *response-ns*  (create-ns 'cljbot.actions.actions))
(def *response-args* '[connection msg client-info])

(defn extract-fn-sym [msg]
  (symbol (:command msg)))

(defn on-msg-dispatch [msg args]
  (let [sym (extract-fn-sym msg)
        callback (ns-resolve *response-ns* sym)]
    (if (not (nil? callback))
      (apply callback args)
      nil)))

(defmacro def-response [name & body]
  (let [sym (symbol name)]
    `(intern cljbot.actions/*response-ns*
             '~sym
             (fn ~*response-args*
               ~@body))))

             
(def-response PING
  (pong connection msg))
  

(def-response JOIN
  (let [my-nick (-> client-info :user :nick)
        other-nick (-> msg :prefix msgs/extract-username)
        channel (:channel client-info)]
    (if (not= my-nick other-nick)
      (say connection channel (str "hello, " other-nick)))))


(def-response PRIVMSG
  (let [my-nick (-> client-info :user :nick)
        other-nick (-> msg :prefix msgs/extract-username)
        target (first (:args msg))
        text (last (:args msg))
        front (apply str (next (take (inc (count my-nick)) text)))]
    (if (= front my-nick)
      (say connection target (str other-nick ":"
                                  (apply str (drop (+ 2 (count my-nick)) text)))))))


(def-response ERROR
   (quit connection))
