### Applicative, Functor in Haskell

[Functors, Applicatives, Monads](http://adit.io/posts/2013-04-17-functors,_applicatives,_and_monads_in_pictures.html)

```haskell
-- Maybe is a functor
fmap (+3) (Just 5) -- 8
fmap (+3) Nothing -- Nothing

-- We can use <$> instead of fmap
import Data.Functor
(+3) <$> Nothing -- Nothing

-- functions are functors
fmap :: (a -> b) -> (r -> a) -> (r -> b)

instance Functor ((->) r) where
    fmap f g = f . g

ghci> fmap (*3) (+100) 1
303

ghci> (*3) . (+100) $ 1
303
```

```haskell
import Control.Applicative

> Just (+3) <*> Just 2
-- Just 5

> [(*2), (*3)] <*> [1, 2, 3]
-- [2,4,6,3,6,9]
```

**Lifting**

```haskell
> (*) <$> Just 5 <*> Just 3
-- Just 15

> liftA2 (*) (Just 5) (Just 3)
Just 15
```

[Controll.Applicative](http://hackage.haskell.org/package/base-4.7.0.1/docs/Control-Applicative.html)

```haskell
liftA :: Applicative f => (a -> b) -> f a -> f b

liftA2 :: Applicative f => (a -> b -> c) -> f a -> f b -> f c
```

### Scala

[Scalaz: Applicative](http://eed3si9n.com/learning-scalaz/Applicative.html)

> Control.Applicative module defines two methods `pure` and `<*>`

```scala
trait Applicative[F[_]] extends Apply[F] { self =>
  def point[A](a: => A): F[A]

  /** alias for `point` */
  def pure[A](a: => A): F[A] = point(a)

  ...
}

scala> 1.point[List]
res14: List[Int] = List(1)

scala> 1.point[Option]
res15: Option[Int] = Some(1)

scala> 1.point[Option] map {_ + 2}
res16: Option[Int] = Some(3)

scala> 1.point[List] map {_ + 2}
res17: List[Int] = List(3)
```

Whereas `fmap` takes a function and a functor and applies th function inside the functor value,

`<*>` takes a functor that has a function in it and another functor and extracts that function from the first functor and then maps it over the second one.

In scala, `Apply` enables to use `<*>`, `<*`, `*>`

```scala
trait Apply[F[_]] extends Functor[F] { self =>
  def ap[A, B](fa: => F[A])(f: => F[A => B]): F[B]
}
```

Notice that functor containing value comes first as the argument. So,

```scala
scala> {(_: Int) + 3}.some
res11: Option[Int => Int] = Some(<function1>)

scala> {(_: Int) + 3}.some <*> 9.some
<console>:14: error: type mismatch;
 found   : Option[Int]
 required: Option[(Int => Int) => ?]
              {(_: Int) + 3}.some <*> 9.some
```

Before Using Applicative

```scala
scala> List(1, 2, 3, 4) map {(_: Int) * (_:Int)}.curried
res11: List[Int => Int] = List(<function1>, <function1>, <function1>, <function1>)

scala> res11 map {_(9)}
res12: List[Int] = List(9, 18, 27, 36)
```

After

```scala
scala> 3.some <*> { 9.some <*> {(_: Int) + (_: Int)}.curried.some }
res15: Option[Int] = Some(12)
```

Applicative Style

```scala
scala> (3.some |@| 9.some) {_ + _}
res16: Option[Int] = Some(12)

scala> (3.some |@| none[Int]) {_ + _}
res18: Option[Int] = None
```

### Lists as Apply

Lists are **applicative functor**

```scala
scala> (List("ha", "heh", "hmm") |@| List("?", "!", ".")) {_ + _}
res63: List[String] = List(ha?, ha!, ha., heh?, heh!, heh., hmm?, hmm!, hmm.)
```

### liftA2, sequenceA

```haskell
-- haskell
liftA2:: (Applicative f) => (a -> b -> c) -> f a -> f b -> f c
```

```scala
val lifted = Apply[Option].lift2((_: Int) :: (_: List[Int]))
val result = lifted(1.some, 2.pure[List].some)

result should be (Some(List(1, 2)))

// also you can
scala> (3.some |@| 4.pure[List].some) {_ :: _}
res28: Option[List[Int]] = Some(List(3, 4))
```

```haskell
-- haskell
sequenceA :: (Applicative f) => [f a] -> f [a]

sequenceA [] = pure []
sequenceA (x:xs) = (:) <$> x <*> sequenceA xs
```

Apply `:` with `x` functor. It will be applicative. So Applying `sequenceA` to rest of the list gives `f [a]`

In scala

```scala
def sequenceA[F[_]: Applicative, A](list: List[F[A]]): F[List[A]] = list match {
  case Nil => (Nil: List[A]).point[F]
  case x :: xs => (x |@| sequenceA(xs)) {_ :: _}
}
```

Interesting example.

```scala
// consider function as an applicative e.g
// ([r - > a]) -> (r -> [a])

type Function1Int[A] = ({type l[A]=Function1[Int, A]})#l[A]
val sequenced =
  sequenceA(List((_: Int) + 3, (_: Int) + 2, (_: Int) + 1): List[Function1Int[Int]])

val result3 = sequenced(3)
result3 should be (List(6, 5, 4))
```
