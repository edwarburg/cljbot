(ns cljbot.messages
  (:require [clojure.contrib.string :as str]))

(defn extract-prefix [msg]
  "Extract the prefix of a valid IRC message."
  (if (= \: (first msg)) ; if it begins with a colon
    (apply str (take-while #(not= \space %) msg)) ; take the first "word"
    ""))


(defn extract-command [msg]
  "Extract the command of a valid IRC message."
  (apply str (take-while #(not= \space %) ; take the first word out of:
                           (if (= \: (first msg)) ; if there's a prefix
                             (next (drop-while #(not= \space %) msg)) ; everything but the prefix
                             msg)))) ; else the whole message

(defn split-trailing [msg]
  (str/split #" :" (apply str (next msg))))

(defn extract-args [msg]
  "Extract the arguments of a valid IRC message."
  (let [[line trailing]  (split-trailing msg)
        args (drop (if (= \: (first msg)) 2 1) ; args are everything after prefix and command
                   (str/split #" " line))]
    (concat args (if (nil? trailing)
                   '()
                   (list (str ":" trailing))))))

(defn extract-trailing [msg]
  (second (split-trailing msg)))

(defn extract-username [msg]
  (let [prefix (extract-prefix msg)]
    (str/drop 1 (first (str/split #"!" prefix)))))


(defn str->msg [msg]
  "Parses a valid IRC message into prefix, command, and arguments."
  {:prefix (extract-prefix msg)
   :command (extract-command msg)
   :args (extract-args msg)})


(defn msg->str [msg]
  "Convert a parsed message into a string."
  (let [prefix (:prefix msg)
        command (:command msg)
        args (:args msg)]
    (str (if (empty? prefix) ; prefix
           ""
           (str prefix " "))
         
         command

         (if (empty? args) ; args
           ""
           (str " "
              (str/join " " (butlast args))
              (str (if (= (count args) 1) ; check if there's only one argument, otherwise
                       ""                 ; we might end up with 2 spaces
                       " ")
                   (last args)))))))
