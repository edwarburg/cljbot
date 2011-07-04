(ns cljbot.actions
  (:use [cljbot.messages :as msgs]
        [cljbot.irc]))

(def *response-ns*  (create-ns 'cljbot.actions.actions))
(def *response-args* '[connection msg client-info])

(defn extract-fn-sym [msg]
  (symbol (str "on-"
               (clojure.contrib.string/lower-case
                (str (:command msg))))))

(defn on-msg-dispatch [msg args]
   (let [sym (extract-fn-sym msg)
         callback (ns-resolve cljbot.actions/*response-ns* sym)]
     (if (not (nil? callback))
       (apply callback args)
       nil)))

(defmacro def-response [name & body]
  (let [sym (symbol (str "on-" (clojure.contrib.string/lower-case (str name))))]
    `(intern cljbot.actions/*response-ns*
             '~sym
             (with-meta (fn ~*response-args*
                          ~@body)
               {:ns cljbot.actions/*response-ns*
                :name '~sym}))))


(def-response PING
  (pong connection msg))
  

(def-response JOIN
  (let [my-nick (-> client-info :user :nick)
        other-nick (-> msg :prefix msgs/extract-username)
        channel (:channel client-info)]
    (if (not= my-nick other-nick)
      (say connection channel (str "hello, " other-nick)))))




(def-response ERROR
   (quit connection))

(comment
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
)
