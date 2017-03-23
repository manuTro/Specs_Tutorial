(ns specs.leverage
  (:require [clojure.spec :as s]
            [clojure.spec.test :as test]
            [clojure.string :as str]
            [clojure.repl :as repl]))


(defn my-index-of [source search]
  (str/index-of source search))

  (my-index-of "foobar" "bar")
  (apply my-index-of ["foobar" "bar"])

;;spec regex
(s/def ::index-of-args (s/cat :source string? :search string?))
;;validation => useful when you want the most unexpensice form of test
;and receive information about the structure of the data
(s/valid? ::index-of-args ["foo" "f"])
(s/valid? ::index-of-args ["foo" 3])
;*********************************************************************************************
;conform => when you want more information about how data matches spec.
;It returns the possibly destructured value of the data
(s/conform ::index-of-args ["foo" "f"]) ;=> {:source "foo", :search "f"}
;unform is the reverse of conform
(s/unform ::index-of-args {:source "foo", :search "f"})
;*********************************************************************************************
;precise error
;explain => when data fails to conform to spec it tell you precisely why
(s/explain ::index-of-args ["foo" 3])
(s/explain-str ::index-of-args ["foo" 3])
(s/explain-data ::index-of-args ["foo" 3])
; ;**********************************************************************************************
; ;SPECS ARE COMPOSABLE
;
(s/explain (s/every ::index-of-args) [["good" "a"]
                                      ["ok" "b"]
                                      ["bad" 42]])
; ; =>In: [2 1] val: 42 fails spec: :specs.spec/index-of-args at: [:search] predicate: string?
; ; nil
; ;returns [2 1] thet is the position of the wrong value
; ;**********************************************************************************************
; ;specs can be used to GENERATE EXAMPLE data
; ;exercise=> it is useful for testing, when you are developing your own specs,
; ;to verify that your specs cover the appropriate domain and when you are trying
; ;to undestand specs written by others
; (s/exercise ::index-of-args)
;
; ;**********************************************************************************************
; ;;ASSERTION
;
(s/check-asserts true) ;enabled assertion
(s/assert ::index-of-args ["foo" "f"])
;(s/assert ::index-of-args ["foo" 42])
;**********************************************************************************************
;SPECING A FUNCTION
(s/fdef my-index-of
                :args (s/cat :source string? :search string?) ;argument of the function
                :ret nat-int? ;the return of the function. in this case a integer >=0;
                :fn #(<= (:ret %) (-> % :args :source count))) ;and the semantic of the function
                ; "the return of the function (:ret) must be <= of the count of
                ;  the elements of the first argument (:source)"
;documetation of the function
(repl/doc my-index-of)
;generative testing
;it returns also the corner case that we maybe didn't consider.
;in this case when args is ("" "0")
(->> (test/check `my-index-of) test/summarize-results)
;***********************************************************************************************
;instrumentation
(test/instrument `my-index-of)
;(my-index-of "foo" 42)
