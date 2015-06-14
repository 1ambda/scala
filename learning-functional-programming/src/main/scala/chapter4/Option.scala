package chapter4


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

case class Some[+A](get: A) extends Option[A]
case object None extends Option[Nothing]

object Chapter4 {
  def variance(xs: Seq[Double]): scala.Option[Double] = {
    mean(xs).flatMap((m: Double) => mean(xs.map(x => math.pow(x - m, 2))))
  }

  def mean(xs: Seq[Double]): scala.Option[Double] =
    if (xs.isEmpty) scala.None else scala.Option(xs.sum / xs.length)
}



