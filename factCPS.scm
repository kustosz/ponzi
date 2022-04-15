(letrec*
  ($$cont2 ($$cont3)
    (letrec*
      ($$cont0 ($$var1) (HALT $$var1))
      ($$cont3 ($$cont0 1) ($$cont0 2))
    )
  )
  (zero? n $$cont2))
