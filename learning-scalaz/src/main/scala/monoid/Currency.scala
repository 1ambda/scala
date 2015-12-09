package monoid

object Currency1 {
  import scalaz._, Scalaz._

  sealed trait Currency
  /* inefficient, as each instance will carry implicit Monoid
   * See, http://stackoverflow.com/questions/34169144/creating-monoids-for-every-subclass-using-scalaz-or-shapeless/34173242#34173242 */
  final case class GBP[A: Monoid](amount: A) extends Currency
  final case class USD[A: Monoid](amount: A) extends Currency
  final case class EUR[A: Monoid](amount: A) extends Currency

  object Implicits {
    implicit class CurrencyOps[A: Monoid](a: A) {
      def GBP = Currency1.GBP(a)
      def EUR = Currency1.EUR(a)
      def USD = Currency1.USD(a)
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
}

object Currency2 {
  import scalaz._
  import shapeless._

  sealed trait Currency extends Any
  final case class GBP[A](amount: A) extends Currency
  final case class USD[A](amount: A) extends Currency
  final case class EUR[A](amount: A) extends Currency

  object Implicits {
    implicit class CurrencyOps[A: Monoid](a: A) {
      def GBP = Currency2.GBP(a)
      def EUR = Currency2.EUR(a)
      def USD = Currency2.USD(a)
    }

    implicit def monoidCurrency[A, C[_] <: Currency]
    (implicit M: Monoid[A], G: Generic.Aux[C[A], A :: HNil]) = new Monoid[C[A]] {
      override def zero: C[A] =
        G.from(M.zero:: HNil)

      override def append(f1: C[A], f2: => C[A]): C[A] = {
        val x = G.to(f1).head
        val y = G.to(f2).head
        G.from(M.append(x, y) :: HNil)
      }
    }
  }
}
