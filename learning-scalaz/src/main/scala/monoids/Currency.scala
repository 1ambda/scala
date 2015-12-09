package monoids

import scalaz._, Scalaz._

final case class GBP[A: Monoid](amount: A)

final class CurrencyOp[A: Monoid](self: A) {
  def GBP: GBP[A] = monoids.GBP(self)
}

trait ToCurrencyOp {
  implicit def toCurrencyOp[A: Monoid](a: A) = new CurrencyOp(a)
}

trait CurrencyMonoid {
  implicit def gbpMonoid[A: Monoid]: Monoid[GBP[A]] = new Monoid[GBP[A]] {
    override def zero =
      GBP(Monoid[A].zero)

    override def append(f1: GBP[A], f2: => GBP[A]): GBP[A] =
      GBP(Semigroup[A].append(f1.amount, f2.amount))
  }
}

object Currency extends ToCurrencyOp with CurrencyMonoid

