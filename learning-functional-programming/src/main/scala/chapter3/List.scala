package chapter3

sealed trait List[+A]
case object Nil extends List[Nothing]
case class Cons[+A](head: A, tail: List[A]) extends List[A]

object List {
  def sum(xs: List[Int]): Int = foldRight(xs)(0)(_ + _)

  def product(ds: List[Double]): Double = foldRight(ds)(1.0)(_ * _)

  def apply[A](as: A*): List[A] =
    if (as.isEmpty) Nil
    else Cons(as.head, apply(as.tail: _*))

  def append[A](xs: List[A], ys: List[A]): List[A] = xs match {
    case Nil => ys
    case Cons(z, zs) => Cons(z, append(zs, ys))
  }

  def has[A](xs: List[A])(value: A): Boolean = xs match {
    case Cons(y, ys) if (y == value) => true
    case Cons(y, ys) => has(ys)(value)
    case Nil => false
  }

  def dropWhile[A](xs: List[A])(f: A => Boolean): List[A] = xs match {
    case Cons(y, ys) if f(y) => dropWhile(ys)(f)
    case _ => xs
  }

  def tail[A](xs: List[A]) = drop(xs)(1)

  def drop[A](xs: List[A])(n: Int): List[A] = xs match {
    case Cons(y, ys) if n > 0 => drop(ys)(n - 1)
    case _ => xs
  }

  def init[A](xs: List[A]): List[A] =  xs match {
    case Nil => Nil
    case Cons(x, Nil) => Nil
    case Cons(y, ys) => Cons(y, init(ys))
  }

  def foldRight[A, B](xs: List[A])(z: B)(f: (A, B) => B): B = xs match {
    case Nil => z
    case Cons(y, ys) => f(y, foldRight(ys)(z)(f))
  }
}




