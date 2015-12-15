package monoid

import org.scalatest._
import util.FunTestSuite

class CurrencySpec extends FunTestSuite {
  test("Currency1: GBP, USD, EUR support plus") {
    import scalaz._, Scalaz._
    import Currency1._, Currency1.Implicits._
    1.GBP |+| 2.GBP shouldBe 3.GBP
    1.USD |+| 2.USD shouldBe 3.USD
    1.EUR |+| 2.EUR shouldBe 3.EUR
  }

  test("Currency2: GBP, USD, EUR support plus") {
    import scalaz._, Scalaz._
    import Currency2._, Currency2.Implicits._
    1.GBP |+| 2.GBP shouldBe 3.GBP
    1.USD |+| 2.USD shouldBe 3.USD
    1.EUR |+| 2.EUR shouldBe 3.EUR
  }
}

