package free.withoutScalaz

sealed trait FreeMonoid1[+A]
final case object Zero1 extends FreeMonoid1[Nothing]
final case class Value[A](a: A) extends FreeMonoid1[A]
final case class Append1[A](l: FreeMonoid1[A], r: FreeMonoid1[A]) extends FreeMonoid1[A]


sealed trait FreeMonoid[+A]
final case object Zero extends FreeMonoid[Nothing]
final case class Append[A](l: A, r: FreeMonoid[A]) extends FreeMonoid[A]

