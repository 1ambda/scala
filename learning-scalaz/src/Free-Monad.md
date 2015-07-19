# Free Monad

[Ref - Advanced Scala 2015 Free Monad](http://noelwelsh.com/assets/downloads/advanced-scala-2015-free-monads.pdf)

## Monad 

Functional Programming is about transforming values. 

> `A => B => C`

FP patterns are just special case of this. 

> `F[A] => F[B] => F[C]`
> `F[A] flatMap (A => F[B])

Monads are about sequencing computation.

## Interpreter

Interpreter separates structure and meaning. **Structure** represent computation as data. For example,

> `1 + 2 + 3 = Add(1, Add(2, 3))`

## Free Monad

`Free Monad = monad + interpreter`

Free monad provides an AST for monadic operations. We can then write custom interpreters.
 
Monad has two operations. `flatMap` and `point`. So AT has two cases, `flatMap` and `point`

The free monad requires that it's payload as a `Functor`. We can construct a `Functor` from any value using the
`Conyoneda`

Free monads are simple. It's just an AST and an interpreter for that AST.




