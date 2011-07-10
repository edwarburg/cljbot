(ns cljbot.messages
  (:require [clojure.contrib.string :as str]))

(defn extract-prefix 
  "Extract the prefix of a valid IRC message."
  [msg]
  (if (= \: (first msg)) ; if it begins with a colon
    (apply str (take-while #(not= \space %) msg)) ; take the first "word"
    ""))


(defn extract-command 
  "Extract the command of a valid IRC message."
  [msg]
  (apply str (take-while #(not= \space %) ; take the first word out of:
                           (if (= \: (first msg)) ; if there's a prefix
                             (next (drop-while #(not= \space %) msg)) ; everything but the prefix
                             msg)))) ; else the whole message


(defn split-trailing
  "Split message on the trailing arguments"
  [msg]
  (str/split #" :" (apply str (next msg))))


(defn extract-trailing 
  "Extract trailing arguments."
  [msg]
  (apply str (drop-while #(not= % \:) (next msg))))


(defn extract-args 
  "Extract the arguments of a valid IRC message."
  [msg]
  (let [[line trailing]  (split-trailing msg)
        args (drop (if (= \: (first msg)) 2 1) ; args are everything after prefix and command
                   (str/split #" " line))]
    (concat args (if (nil? trailing)
                   '()
                   (list (extract-trailing msg))))))


(defn extract-username
  "Extract username from a valid IRC message."
  [msg]
  (let [prefix (extract-prefix msg)]
    (str/drop 1 (first (str/split #"!" prefix)))))


(defn str->msg 
  "Parses a valid IRC message into prefix, command, and arguments."
  [msg]
  {:prefix (extract-prefix msg)
   :command (extract-command msg)
   :args (extract-args msg)})


(defn msg->str 
  "Convert a parsed message into a string."
  [msg]
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
