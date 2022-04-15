(((lambda (x) (x x))
  (lambda (fact-gen)
    (lambda (n)
      (if (zero? n)
          1
          (* n ((fact-gen fact-gen) (subtract n 1)))))))
 5)