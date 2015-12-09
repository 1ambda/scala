package monoid

import scalaz._, Scalaz._


sealed trait Currency
final case class GBP[A: Monoid](amount: A) extends Currency
final case class USD[A: Monoid](amount: A) extends Currency
final case class EUR[A: Monoid](amount: A) extends Currency

object Implicits {
    implicit class CurrencyOps[A: Monoid](a: A) {
      def GBP = monoid.GBP(a)
      def EUR = monoid.EUR(a)
      def USD = monoid.USD(a)
    }

    implicit def gbpMonoid[A: Monoid]: Monoid[GBP[A]] = new Monoid[GBP[A]] {
      override def zero =
        GBP(Monoid[A].zero)

      override def append(f1: GBP[A], f2: => GBP[A]): GBP[A] =
        GBP(Semigroup[A].append(f1.amount, f2.amount))
    }

    implicit def usdMonoid[A: Monoid]: Monoid[USD[A]] = new Monoid[USD[A]] {
      override def zero =
        USD(Monoid[A].zero)

      override def append(f1: USD[A], f2: => USD[A]): USD[A] =
        USD(Semigroup[A].append(f1.amount, f2.amount))
    }

    implicit def eurMonoid[A: Monoid]: Monoid[EUR[A]] = new Monoid[EUR[A]] {
      override def zero =
        EUR(Monoid[A].zero)

      override def append(f1: EUR[A], f2: => EUR[A]): EUR[A] =
        EUR(Semigroup[A].append(f1.amount, f2.amount))
    }
}
