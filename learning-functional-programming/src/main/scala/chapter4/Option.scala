package chapter4

import scala.annotation.tailrec

trait Option[+A] {
  def map[B](f: A => B): Option[B] = this match {
    case Some(elem) => Some(f(elem))
    case None       => None
  }

  def flatMap[B](f: A => Option[B]): Option[B] = this match {
    case Some(elem) => f(elem)
    case None       => None
  }
  def filter(f: A => Boolean): Option[A] = this match {
    case Some(elem) if f(elem) => this
    case _                     => None
  }

  def getOrElse[B >: A](default: => B): B = this match {
    case Some(elem) => elem
    case None => default
  }
  def orElse[B >: A](ob: => Option[B]): Option[B] = this match {
    case None => ob
    case _ => this
  }

  def lift[A, B](f: A => B): Option[A] => Option[B] = _ map f
  val absO: Option[Double] => Option[Double] = lift(math.abs)
}

object Option {
  def Try[A](a: => A): Option[A] = {
    try Some(a)
    catch { case e: Exception => None }
  }

  def map2[A, B, C](a: Option[A], b: Option[B])(f: (A, B) => C): Option[C] = {
    for {
      aValue <- a
      bValue <- b
    } yield f(aValue, bValue)
  }

  def sequence_1[A](a: List[Option[A]]): Option[List[A]] = a match {
    case Nil => Some(Nil)
    case x :: xs => x flatMap(xValue => sequence_1(xs) map (xValue :: _))
  }

  // simply,
  def sequence_2[A](a: List[Option[A]]): Option[List[A]] =
    a.foldRight[Option[List[A]]](Some(Nil))((x,y) => map2(x,y)(_ :: _))

  // sequence using traverse
  def sequence[A](a: List[Option[A]]): Option[List[A]] = {
    traverse(a)(x => x)
  }

  def traverse_1[A, B](a: List[A])(f: A => Option[B]): Option[List[B]] = {
    // iterate twice. not efficient
    sequence(a map f)
  }

  def traverse_2[A, B](a: List[A])(f: A => Option[B]): Option[List[B]] = a match {
    case Nil => Some(Nil)
    case x :: xs => f(x) flatMap(xValue => traverse_2(xs)(f) map(xValue :: _))
  }

  // simply,
  def traverse[A, B](a: List[A])(f: A => Option[B]): Option[List[B]] = {
    a.foldRight[Option[List[B]]](Some(Nil))((x, y) => map2(f(x), y)(_ :: _))
  }
}

case class Some[+A](get: A) extends Option[A]
case object None extends Option[Nothing]


