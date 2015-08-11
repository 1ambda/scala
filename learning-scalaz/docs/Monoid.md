# Monoid


`Monoid` extends `Semigroup`

```scala
/**
 * Provides an identity element (`zero`) to the binary `append`
 * operation in [[scalaz.Semigroup]], subject to the monoid laws.
 *
 * Example instances:
 *  - `Monoid[Int]`: `zero` and `append` are `0` and `Int#+` respectively
 *  - `Monoid[List[A]]`: `zero` and `append` are `Nil` and `List#++` respectively
 */
 
trait Monoid[F] extends Semigroup[F] { self =>
  ...
```

*Semigroup* is an associative binary operation, circumscribed by type and the semigroup laws.  Unlike [[scalaz.Monoid]], 
there is not necessarily a zero.

```scala
trait Semigroup[F]  { self =>
  def append(f1: F, f2: => F): F
  ...
}
```

