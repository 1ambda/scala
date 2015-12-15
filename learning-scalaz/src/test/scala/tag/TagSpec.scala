package tag

import util.{WordTestSuite, FunTestSuite}

/**
 * ref - http://eed3si9n.com/learning-scalaz/Tagged+type.html
 */

class TagSpec extends WordTestSuite {

    "Creating Tagged type" in {

      import scalaz._, Scalaz._, Tag._, Tags._, syntax.tag._

      sealed trait USD
      sealed trait EUR
      def USD[A](amount: A): A @@ USD = Tag[A, USD](amount)
      def EUR[A](amount: A): A @@ EUR = Tag[A, EUR](amount)

      val oneUSD = USD(1)
      2 * oneUSD.unwrap shouldBe 2

      def convertUSDtoEUR[A](usd: A @@ USD, rate: A)
                            (implicit M: Monoid[A @@ Multiplication]): A @@ EUR =
        EUR((Multiplication(usd.unwrap) |+| Multiplication(rate)).unwrap)

      // since ===, shouldBe in scalatest only check runtime values we need =:=
    convertUSDtoEUR(USD(1), 2) =:= EUR(2)
    // convertUSDtoEUR(USD(1), 2) =:= EUR(3) // will fail
    // convertUSDtoEUR(USD(1), 2) =:= USD(3) // compile error
    }

  "without Scalaz" in {
    type Tagged[T] = { type Tag = T }
    type @@[A, T] = A with Tagged[T]
  }
}

