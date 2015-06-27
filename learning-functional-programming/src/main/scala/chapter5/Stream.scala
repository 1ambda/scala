package chapter5


import scala.{Option => _, Either => _, Left => _, Right => _, _} // hide std library `Option` and `Either`, since we are writing our own in this chapter

trait Stream[+A]
case object Empty extends Stream[Nothing]
case class Cons[+A](h: () => A, t: () => Stream[A]) extends Stream[A]

object Stream {
  def empty = Empty

  def cons[A](h: => A, t: => Stream[A]) = {
    lazy val head = h
    lazy val tail = t

    Cons(() => head, () => tail)
  }

  def apply[A](as: A*): Stream[A] =
    if (as.isEmpty) Empty else cons(as.head, apply(as.tail: _*))

  // invalid cons implementation
  def cons2[A](h: => A, t: => Stream[A]) = {
    // h, t will be evaluated whenever accesses
    Cons(() => h, () => t)
  }
}

