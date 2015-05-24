package chapter1

import org.scalatest.{FunSuite, Matchers}

class CafeSpec extends FunSuite with Matchers {

  test("buying 10 coffees") {
    val cafe = new Cafe
    val card = new CreditCard

    val (coffees, charge) = cafe.buyCoffee(card, 10)

    coffees should have length (10)
    charge.price should be (15 * 10)
  }
}

