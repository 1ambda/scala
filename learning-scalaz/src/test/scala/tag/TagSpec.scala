package tag

import util.TestSuite

import scalaz._, Scalaz._, Tag._, Tags._, syntax.tag._

/**
 * ref - http://eed3si9n.com/learning-scalaz/Tagged+type.html
 */

class TagSpec extends TestSuite {

    test("Creating Tagged type") {
      sealed trait USD
      sealed trait EUR
      def USD[A](amount: A): A @@ USD = Tag[A, USD](amount)
      def EUR[A](amount: A): A @@ EUR = Tag[A, EUR](amount)

      val oneUSD = USD(1)
      2 * oneUSD.unwrap shouldBe 2

      def convertUSDtoEUR[A](usd: A @@ USD, rate: A)
                            (implicit M: Monoid[A @@ Multiplication]): A @@ EUR =
        EUR((Multiplication(usd.unwrap) |+| Multiplication(rate)).unwrap)

      convertUSDtoEUR(USD(1), 2) === EUR(2)
      convertUSDtoEUR(USD(1), 2) shouldBe EUR(2)

      // tagged types are treated as subtype of the origin type
      2 shouldBe USD(2)
      convertUSDtoEUR(USD(1), 2) shouldBe USD(2)
      convertUSDtoEUR(USD(1), 2) === USD(2)
    }

  test("without Scalaz") {
    type Tagged[T] = { type Tag = T }
    type @@[A, T] = A with Tagged[T]
  }
}
