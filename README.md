# cljbot

cljbot: a simple IRC bot written in Clojure.

# Usage

As this is only a personal project, end users are kind of on their own here. The easiest way to do this is the following:

* Install emacs, clojure-mode, leiningen and swank-clojure.
* Open core.clj.
* Run M-x clojure-jack-in, and compile core.clj with C-c C-k.
* Create a connection with cljbot.core/connect, and manipulate it with functions from cljbot.irc.
