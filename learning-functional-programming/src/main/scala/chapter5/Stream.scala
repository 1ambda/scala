package chapter5

import scala.annotation.tailrec
import scala.{Option => _, Either => _, Left => _, Right => _, _} // hide std library `Option` and `Either`, since we are writing our own in this chapter
import scala.collection.immutable.{Stream => _, List => _}

trait Stream[+A] {
  import Stream._

  def toList: List[A] = this match {
    case Cons(h, t) => h() :: t().toList
    case _ => Nil
  }

  def exists(p: A => Boolean): Boolean =
    foldRight(false)((h, t) => p(h) || t)

  def exists2(p: A => Boolean): Boolean = this match {
    case Cons(h, t) => p(h()) || t().exists2(p)
    case _ => false
  }

  def foldRight[B](z: => B)(f: (A, => B) => B): B = this match {
    case Cons(h, t) => f(h(), t().foldRight(z)(f))
    case _ => z
  }

  def forAll2(p: A => Boolean): Boolean = this match {
    case Cons(h, t) => p(h()) && t().forAll2(p)
    case _ => true
  }

  def forAll(p: A => Boolean): Boolean =
    foldRight(true)((a, b) => p(a) && b)

  def map2[B](f: A => B): Stream[B] = this match {
    case Cons(h, t) => Cons(() => f(h()), () => t().map2(f))
    case _ => Empty
  }

  def map[B](f: A => B): Stream[B] =
    foldRight(empty[B])((a, b) => cons(f(a), b))

  def filter2(p: A => Boolean): Stream[A] = this match {
    case Cons(h, t) =>
      if (!p(h())) t().filter(p)
      else cons(h(), t().filter2(p))
    case _ => Empty
  }

  def filter(p: A => Boolean): Stream[A] =
    foldRight(empty[A])((a, b) => if (p(a)) cons(a, b) else b)

  def append2[B >: A](s: => Stream[B]): Stream[B] = this match {
    case Empty => s
    case Cons(h, t) => cons(h(), t().append2(s))
  }

  def append[B >: A](s: => Stream[B]): Stream[B] =
    foldRight(s)((h, t) => cons(h, t))

  def flatMap2[B](f: A => Stream[B]): Stream[B] = this match {
    case Cons(h, t) => f(h()) append t().flatMap2(f)
    case _ => empty[B]
  }

  def flatMap[B](f: A => Stream[B]): Stream[B] =
    foldRight(empty[B])((h, t) => f(h) append t)

  def take(n: Int): Stream[A] = this match {
    case Cons(h, t) if n != 0 => cons(h(), t().take(n - 1))
    case _ => Empty
  }

}

case object Empty extends Stream[Nothing]
case class Cons[+A](h: () => A, t: () => Stream[A]) extends Stream[A]

object Stream {
  def empty[A]: Stream[A] = Empty

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

  def constant2[A](n: A): Stream[A] = {
    cons(n, constant(n))
  }

  def from2(n: Int): Stream[Int] = {
    cons(n, from2(n + 1))
  }

  def fibs2: Stream[Int] = {

    def recur(x: Int, y: Int): Stream[Int] = {
      cons(x, recur(y, x + y))
    }

    recur(0, 1)
  }

  def constant[A](n: A): Stream[A] = unfold(n)(_ => Some((n, n)))

  def from(n: Int): Stream[Int] = unfold(n)(n => Some((n, n + 1)))

  def fibs: Stream[Int] = unfold((0, 1)) { case (x, y) =>
      Some((x, (y, x + y)))
  }

  def unfold[A, S](z: S)(f: S => Option[(A, S)]): Stream[A] = f(z) match {
    case Some((a, ns)) => cons(a, unfold(ns)(f))
    case None => empty
  }
}



