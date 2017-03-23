(ns specs.testing
  (:require [clojure.spec :as s]
            [clojure.spec.test :as test]
            [clojure.string :as str]
            [clojure.repl :as repl]))


;test/check => generate conforming inputs
;              check returns

;test/instrument => check conforming inputs, i.e.
;make sure the implementation of the function is correct

;fn under test
(defn my-index-of
  "Returns the index at which search appears in source"
  [source search & opts]
  (apply str/index-of source search opts))
;********************************************************************************
; Now I define a specs for my function
(s/fdef my-index-of
                :args (s/cat :source string? :search string?) ;argument of the function
                :ret nat-int? ;the return of the function. in this case a integer >=0;
                :fn #(<= (:ret %) (-> % :args :source count))) ;and the semantic of the function
;exercise-fn returns data with the args and the result
(s/exercise-fn `my-index-of)
;********************************************************************************
;alt => alternative regular expression, i.e. the oarameter can be
; a string or either a char
(s/conform (s/alt :string string? :char char?) ["foo"]) ;=> [:string "foo"]
(s/explain (s/alt :string string? :char char?) [\f]) ;=> [:char \f]
; (s/conform (s/alt :string string? :char char?) [42]) => invalid
;********************************************************************************
;; alt in args
(s/fdef my-index-of
                :args (s/cat :source string?
                             :search (s/alt :string string?
                                            :char char?))
                :ret nat-int?
                :fn #(<= (:ret %) (-> % :args :source count)))

(s/exercise-fn `my-index-of)
;********************************************************************************
;;quantification operator ?
(s/conform (s/? nat-int?)[])
(s/conform (s/? nat-int?)[1])
(s/explain (s/? nat-int?)[:a])
(s/explain (s/? nat-int?)[1 2])
;now we can put it in our function
(s/fdef my-index-of
                :args (s/cat :source string?
                             :search (s/alt :string string?
                                                            :char char?)
                             :from (s/? nat-int?))
                :ret nat-int?
                :fn #(<= (:ret %) (-> % :args :source count)))
;;example testing
(assert (= 8 (my-index-of "testing with specs" "w")))
;this king of test is biring and repetitive
;; test/check generative testing
(->> (test/check `my-index-of) (test/summarize-results))

;;nilable spec => allows also nil as avaiable string
(s/conform (s/nilable string?) "foo") ;=> "foo"
(s/conform (s/nilable string?) nil)  ;=> nil
(s/conform (s/nilable string?) 42)  ;=> invalid

;we can correct my-index-of function with nilable
(s/fdef my-index-of
                :args (s/cat :source string?
                             :search (s/alt :string string?
                                                            :char char?)
                             :from (s/? nat-int?))
                :ret (s/nilable nat-int?)
                :fn #(<= (:ret %) (-> % :args :source count)))
;If we run:
(->> (test/check `my-index-of) (test/summarize-results))
;we will obtain a NullPointerException.
;This because if fn have ret = nil, we have an exception.
;So we have to manage it, with or specs
;; or specs
(s/fdef my-index-of
                :args (s/cat :source string?
                             :search (s/alt :string string?
                                            :char char?)
                             :from (s/? nat-int?))
                :ret (s/nilable nat-int?)
                :fn (s/or
                      :not-found #(nil? (:ret %))
                      :found #(<= (:ret %) (-> % :args :source count))))

(->> (test/check `my-index-of) (test/summarize-results) ;now it is ok
 ;******************************************************************************
; Let's define an high level function that use my-index-of function on its implementation
(defn which-came-first
  "Returns :chicken or :egg, depending on which string appears
  first in s, starting from position from"
  [s from]
  (let [c-idx (my-index-of s "chicken" :from from)
        e-idx (my-index-of s "egg" :from from)]
        (cond
          (< c-idx e-idx) :chicken
          (< e-idx c-idx) :egg)))

(which-came-first "the chicken or the egg" 0)
;oh oh, we have an arity exception
;in order to debug it, call
(pst)
;pst Prints a stack trace of the exception.
;but there is a easiest way, i.e.
(test/instrument `my-index-of)
;after calling instrument, let's use again the function
(which-came-first "the chicken or the egg" 0)
;we will see exactly where the problem is:

; ExceptionInfo Call to #'specs.testing/my-index-of did not conform to spec:
; In: [2] val: :from fails at: [:args :from] predicate: nat-int?
; :clojure.spec/args  ("the chicken or the egg" "chicken" :from 0)
; :clojure.spec/failure  :instrument

;so we are passing ("the chicken or the egg" "chicken" :from 0) at the function my-index-of
;and there is an arity exception

;A more powerful tools for debug is to use check and summarize-results together
;;test + instrument
(s/fdef which-came-first
  :args (s/cat :source string? :from nat-int?)
  :ret #{:chicken :egg})

(->> (test/check `which-came-first) (test/summarize-results))
