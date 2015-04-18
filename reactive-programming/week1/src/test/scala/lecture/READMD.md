# Week1 - Monad

> A **monad** `M` is a parametric type `M[T]` with two operations, `flatMap` (bind) and `unit`, that have to satisfy some laws

```scala
trait M[T] {
  def flatMap[U](f: T => M[U]): M[U]
  }

def unit[T](x: T): M[T]
```

`flatMap` is an operation on each of these types, where as `unit` in Scala is different for each monad. (e.g `List(x)`, `Set(x)`, `Some(x)`, `single(x)`)

`map` can be defined for every monad as a combination of `flatMap` and `unit` (*functor*)

```scala
// map[S](f: T => S): M[S]
// flatMap[S](f: T => M[S]): M[S]
m map f == m flatMap (x => unit(f(x)))
        == m flatMap (f andThen unit)
```

### Monad Laws

To qualify a a monad, a type has to satisfy three laws

(1) **Associativity**

```scala
(m flatMap f) flatMap g == m flatMap (x => f(x) flatMap g)
```

(2) **Left Unit**

```scala
unit(x) flatMap f == f(x)
```

(3) **Right Unit**

```scala
m flatMap unit == m
```

### Option

[Scala 2.11.x Option Impl](https://github.com/scala/scala/blob/2.11.x/src/library/scala/Option.scala#L333)

```scala
final case class MySome[+A](x: A) extends MyOption[A]
final case object MyNone extends MyOption[Nothing] 

abstract class MyOption[+T] {
  def flatMap[U](f: T => MyOption[U]): MyOption[U] = this match {
    case MySome(x) => f(x)
    case MyNone => MyNone
  }
}
```

### Significance of the Laws for For-Expression

(1) **Associativity** says

```scala
for (y <- for (x <- m; y <- f(x)) yield y
     z <- g(y)) yield z

// same as
for (x <- m
     y <- f(x)
     z <- g(y)
) yield z
```

(2) **Right Unit** says

```scala
for (x <- m) yield x == m
```

(3) **Left Unit** dosen't not have an analogue for for-expression

### Try

[Scala 2.11.x Try Impl](https://github.com/scala/scala/blob/2.11.x/src/library/scala/util/Try.scala)

```scala
object MyTry {
  // ref: https://github.com/scala/scala/blob/2.11.x/src/library/scala/util/Try.scala
  import scala.util.control.NonFatal

  abstract class MyTry[+T] {
    def flatMap[U](f: T => MyTry[U]): MyTry[U] = this match {
      case MySuccess(x) => try f(x) catch { case NonFatal(ex) => MyFailure (ex) }
      case fail: MyFailure => fail
    }

    // t map f == t flatMap(f andThen MyTry)
    def map[U](f: T => U): MyTry[U] = this match {
      case MySuccess(x) => MyTry(f(x))
      case fail: MyFailure => fail
    }
  }


  case class MySuccess[T](x: T) extends MyTry[T]
  case class MyFailure(ex: Throwable) extends MyTry[Nothing]

  def apply[T](expr: => T): MyTry[T] =
    try MySuccess(expr)
    catch {
      case NonFatal(ex) => MyFailure(ex)
    }
}
```

In `Try`, It turns out the **left unit* law fails.

```scala
Try(expr) flatMap f != f(expr)
```

Left-hand side will never raise a non-fatal exception whereas the right-hand side will raise any exception thrown by `expr` of `f`

So, `Try` trades one moand law for another law which is more useful in this context

> An expression composed from `Try`, `map`, `flatMap` will never throw a non-fatal exception. This is the "bullet-proof" principle

