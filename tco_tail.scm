(letrec*
  (
    (sum-tc
      (lambda (n acc)
        (if (zero? n)
            acc
            (sum-tc (subtract n 1) (add n acc))
        )
      )
    )
    (sum (lambda (n) (sum-tc n 0)))
  )
  (sum 1000000)
)