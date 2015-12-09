package monoid

import org.scalatest._
import util.TestSuite
import scalaz._, Scalaz._

class CurrencySpec extends TestSuite {
  import Implicits._

  test("GBP support plus") {
    1.GBP |+| 2.GBP shouldBe 3.GBP
  }
}

