(ns specs.generators
  (:require [clojure.spec :as s]
            [clojure.spec.test :as test]
            [clojure.string :as str]
            [clojure.repl :as repl]
            [clojure.spec.gen :as gen]))

;; arbitrary predicates are not efficient constructors
(s/def ::id (s/and string?
                    #(str/starts-with? % "FOO-")))
(s/exercise ::id)
;with this we will have an exception that says thay it's not able to create a random
;string that starts with FOO after 100 tries. So lets create a smarter generators
;; transform exixting generator
(defn foo-gen []
  (->> (s/gen (s/int-in 1 100))
        (gen/fmap #(str "FOO-" %))))
;fmap will take an existing generator and apply a functional transformation.
; we create the  (s/int-in 1 100) generator, and then we'll pass it to fmap
; that apply the transformation of add the FOO- string to it.
;so now it will generate 10 id with FOO-n, where n is a random number from 1 to 100
(s/exercise ::id 10 {::id foo-gen})
;so we passed to ::id 10 the overrided generator ::id foo-gen
;*********************************************************************************
;we can also specify generators when you CREATE specs
;;add generator to spec registry
(s/def ::id
  (s/spec (s/and string?
                #(str/starts-with? % "FOO-"))
                :gen foo-gen))
;specs has the :gen keyword to override the generator.
; so in exercise we can pass id without the generator
(s/exercise ::id)

;********************************************************************************
;often we have dictionary or lookup.
; lookup is a map from keyword to string
;;lookup

(s/def ::lookup (s/map-of keyword? string? :min-count 1))
; so let's generate a random map with one keyword and one string.
;we will have a lot of values.
(s/exercise ::lookup)
;so lets add a condiction.
;lets generate just values s.t the keywords is contained into the lookup.

;; dependent values
(s/def ::lookup-finding-k (s/and (s/cat :lookup ::lookup
                                         :k keyword?)
                                         (fn [{:keys [lookup k]}]
                                           (contains? lookup k))))
(s/exercise ::lookup-finding-k)
;and again we have the 100 tries exception.

;********************************************************************************

;; generate and bind a model
;;bind take two things: a generator based on a model and then a generator that
;;takes that model and trasforms it additional generator into
;useful things in our domain
(defn lookup-finding-k-gen
  []
  (gen/bind (s/gen ::lookup)
              #(gen/tuple
                (gen/return %)
                (gen/elements (keys %)))))

(s/exercise ::lookup-finding-k 10 {::lookup-finding-k lookup-finding-k-gen})
;now the generated data is conformant because une of the things that we aspect it does is
;;make a conforming to check after generating any data. if it doesn't we will have again
;; the 100 undred exception

;now we can come back to the first example

(defn my-index-of
"Returns the index at which search appears in source"
   [source search]
  (str/index-of source search))

  (s/fdef my-index-of
    :args (s/cat :source string?
                  :search string?))
(s/exercise-fn `my-index-of)
;it will find the degenerate cases and all sorts of non-matching strenght
;because it is TOO random.

;; constructively generate a string and substring
(def model (s/cat :prefix string? :match string? :suffix string?))
(defn gen-string-and-substring
  []
  (gen/fmap
    (fn [[prefix match suffix]] [(str prefix match suffix) match])
    (s/gen model)))
  ;;always genrate a findable string
(s/def ::my-index-of-args (s/cat :source string?
                                 :search string?))
(s/fdef my-index-of
            :args (s/spec ::my-index-of-args
                          :gen gen-string-and-substring))
(s/exercise-fn `my-index-of)
;; now we have the opposite case, i.e. all the data have a match.
;; we want a generator thet covers both the check-asserts

;;combines models with one of
(defn gen-my-index-of-args
  []
  (gen/one-of [(gen-string-and-substring)
                (s/gen ::my-index-of-args)])
  )
  (s/fdef my-index-of
    :args (s/spec (s/cat :source string?
                          :search string?)
                          :gen gen-my-index-of-args))
  (s/exercise-fn `my-index-of)

  ;;So now we will have boh the case: the degenerate case,
  ;;the good cases, and the non-matches cases
