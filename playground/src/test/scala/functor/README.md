## Scala Functor

### Haskell Functor

[adit.io: Functor, Applicative, Monad](http://adit.io/posts/2013-04-17-functors,_applicatives,_and_monads_in_pictures.html)

[Haskell Wiki: Functor](https://wiki.haskell.org/Functor)

```haskell
class Functor f where
  fmap :: (a -> b) -> f a -> f b
```

즉 함수 `a->b` 와 컨테이너 `f a` 를 받아, 함수를 적용해서 `f b` 를 돌려줌. 모든 **Functor** 인스턴스는 다음 룰을 따라야 함

```haskell
fmap id      = id
fmap (p . q) = (fmap p) . (fmap q)
```

즉 *id* 와 *function composition* 을 따르면 됌.

[Haskell WikiBooks: Functor](http://en.wikibooks.org/wiki/Haskell/The_Functor_class)

`Maybe`, `[]` 의 경우

```haskell
instance  Functor Maybe  where
    fmap f Nothing    =  Nothing
    fmap f (Just x)   =  Just (f x)

instance  Functor []  where
    fmap = map

> fmap (2*) [1, 2, 3, 4]
[2, 4, 6, 8]

> fmap (2*) (Just 1)
Just 2
```

### Scalaz Functor

[Learning Scalaz: Functor](http://eed3si9n.com/learning-scalaz/Functor.html)

[Scala Functor Impl](https://github.com/scalaz/scalaz/blob/series/7.1.x/core/src/main/scala/scalaz/Functor.scala)

```scala
trait Functor[F[_]] extends InvariantFunctor[F] {
  def map[A, B](fa: F[A])(f: A => B): F[B]

  ...
}

// injected operators
trait FunctorOps[F[_],A] extends Ops[F[A]] {
  implicit def F: Functor[F]
  ////
  import Leibniz.===

  final def map[B](f: A => B): F[B] = F.map(self)(f)

  ...
}
```


```scala
"List" should "be functor" in  {
  val expected = List(1, 2, 3, 4) map {_ + 1}
  expected should be (List(2, 3, 4, 5))
}

"Tuple" should "be functor. but f must be applied to the last member of the tuple" in {
  val expected = (1, 2, 3) map {_ * 2}
  expected should be ((1, 2, 6))
}
```

Also, Function is a functor

```scala
"Function" should "be functor" in  {
  val f = ((x: Int) => x + 1)
  val g = f map {_ * 7}

  val expected = g(3)

  expected should be ((3 + 1) * 7)
}
```

How are functions functors?

```haskell
fmap :: (a -> b) -> (r -> a) -> (r -> b)

ghci> fmap (*3) (+100) 1
303

ghci> (*3) . (+100) $ 1
303
```

In Haskell, the fmap seems to be working as the same orderas `f compose g`. But scala's `map` is not working that way.

```scala
scala> (((_: Int) * 3) map {_ + 100}) (1)
res40: Int = 103
```

The order is reversed. Since `map` is an injected method of `F[A]`, the data stucture to be mapped over coms first, then the function comes next.

```haskell
ghci> fmap (*3) [1, 2, 3]
[3, 6, 9]
```

```scala
trait Functor[F[_]]  { self =>
  /** Lift `f` into `F` and apply to `F[A]`. */
  def map[A, B](fa: F[A])(f: A => B): F[B]

  ...
}

trait FunctorOps[F[_],A] extends Ops[F[A]] {
  implicit def F: Functor[F]
  ////
  import Leibniz.===

  final def map[B](f: A => B): F[B] = F.map(self)(f)

  ...
}

List(1, 2, 3) map { 3 * }
```

### Lifting

We can thing of `fmap` as a function that take a function and returns a new function.

This is called **lifting** a function

```haskell
-- (a -> b) -> (f a -> f b)

ghci> :t fmap (*2)
fmap (*2) :: (Num a, Functor f) => f a -> f a

ghci> :t fmap (replicate 3)
fmap (replicate 3) :: (Functor f) => f a -> f [a]
```

```scala
val lifted = Functor[List].lift { (_: Int) * 2 }
val expected = lifted(List(3))

expected should be (List(6))
```
