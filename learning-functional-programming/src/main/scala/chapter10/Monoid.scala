package chapter10

import chapter8.{Prop, Gen}

trait Monoid[A] {
  def op(a1: A, a2: A): A
  def zero: A
}

object Monoid {
  implicit val stringMonoid = new Monoid[String] {
    override def op(a1: String, a2: String): String = a1 + a2
    override def zero: String = ""
  }

  implicit def listMonoid[A] = new Monoid[List[A]] {
    override def op(a1: List[A], a2: List[A]): List[A] = a1 ++ a2
    override def zero: List[A] = Nil
  }

  implicit val intAddition = new Monoid[Int] {
    override def op(a1: Int, a2: Int): Int = a1 + a2
    override def zero: Int = 0
  }

  implicit val intMultiplication = new Monoid[Int] {
    override def op(a1: Int, a2: Int): Int = a1 * a2
    override def zero: Int = 1
  }

  implicit val booleanOr = new Monoid[Boolean] {
    override def op(a1: Boolean, a2: Boolean): Boolean = a1 || a2
    override def zero: Boolean = false
  }

  implicit val booleanAnd = new Monoid[Boolean] {
    override def op(a1: Boolean, a2: Boolean): Boolean = a1 && a2
    override def zero: Boolean = true
  }

  implicit def optionMonoid[A] = new Monoid[Option[A]] {
    override def op(x: Option[A], y: Option[A]): Option[A] = x orElse y
    override def zero: Option[A] = None
  }

  implicit def endoMonoid[A] = new Monoid[A => A] {
    override def op(f: A => A, g: A => A): A => A = f compose g
    override def zero: (A) => A = a => a
  }

  def dual[A](implicit m: Monoid[A]): Monoid[A] = new Monoid[A] {
    override def op(x: A, y: A): A = m.op(y, x)
    override def zero: A = m.zero
  }

  def concatenate[A](as: List[A])(implicit m: Monoid[A]): A =
    as.foldLeft(m.zero)(m.op)

  def foldMap[A, B](as: List[A])(f: A => B)(m: Monoid[B]): B =
    as.foldLeft(m.zero)((b, a) => m.op(b, f(a))) // concatenate(as map f)

  def foldRight[A, B](as: List[A])(z: B)(f: (A, B) => B): B =
    foldMap(as)(f.curried)(endoMonoid[B])(z)

  def foldLeft[A, B](as: List[A])(z: B)(f: (B, A) => B): B =
    foldMap(as)(a => (b: B) => f(b, a))(dual(endoMonoid[B]))(z)

  def foldMapV[A, B](v: IndexedSeq[A])(m: Monoid[B])(f: A => B): B = v.length match {
    case 0 => m.zero
    case 1 => f(v(0))
    case _ =>
      val (left, right) = v.splitAt(v.length / 2)
      m.op(foldMapV(left)(m)(f), foldMapV(right)(m)(f))
  }
}

















