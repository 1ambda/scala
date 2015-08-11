# Natural Transformer

[Ref 1 - Scalaz](https://github.com/scalaz/scalaz/blob/series/7.2.x/core/src/main/scala/scalaz/NaturalTransformation.scala]
[Ref 2 - Learning Scala](http://eed3si9n.com/learning-scalaz/Natural-Transformation.html)

## Basics

A universally quantified function, usually written as `F ~> G`

Natural Transformer can be used to encode first-class functor transformations in the same way
functions encode first-class concrete value morphisms.


```scala
trait NaturalTransformation[-F[_], +G[_]] {
self =>
def apply[A](fa: F[A]): G[A]

def compose[E[_]](f: E ~> F): E ~> G = new (E ~> G) {
  def apply[A](ea: E[A]) = self(f(ea))
}

def andThen[H[_]](f: G ~> H): F ~> H =
  f compose self
}

type ~>[-F[_], +G[_]] = NaturalTransformation[F, G]
type <~[+F[_], -G[_]] = NaturalTransformation[G, F]
```

## Motivations

> A natural transformation is a morphism of functions. For fix categories `C` and `D`, 
we can regard the functors `C => D` as the object of a new category, and the arrows between
these objects are what we are going to call **natural transformations**
 
> We are not able to define a function that maps an `Option[T]` to `List[T]` for every `T`

