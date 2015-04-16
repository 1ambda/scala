package coursera.chapter2

import org.scalatest._

class FunctionsAndDataTest extends FlatSpec with Matchers {

  "new Rational(3, 4)" should "have 3 as numer and 4 as denom" in {
    val r1 = new Rational(3, 4)
    assert(r1.numer == 3)
    assert(r1.denom == 4)
  }

  "1/4 + 2/4" should "be eql 3/4" in {
    val r1 = new Rational(1, 4)
    val r2 = new Rational(2, 4)
    val r3 = r1 + r2

    assert(r3.numer == 3)
    assert(r3.denom == 4)
  }

  "Rational(1, 4).neg" should "return Rational(-1, -4)" in {
    val r1 = new Rational(1, 4)
    val r2 = -r1
    assert(r2.numer == -1)
    assert(r2.denom == 4)
  }

  "1/4 - 2/4" should "be eql -1/4" in {
    val r1 = new Rational(1, 4)
    val r2 = new Rational(2, 4)
    val r3 = r1 - r2

    assert(r3.numer == -1)
    assert(r3.denom == 4)
  }

  "1/4 < 2/4" should "be true" in {
    val r1 = new Rational(1, 4)
    val r2 = new Rational(2, 4)

    assert(r1 < r2)
  }

  "(1/4).max(2/4)" should "be (2/4)" in {
    val r1 = new Rational(1, 4)
    val r2 = new Rational(2, 4)

    assert(r1.max(r2) == r2)
  }

  "Rational(1, 0)" should "throw IllegalArgumentException" in {
    intercept[IllegalArgumentException] {
      new Rational(1, 0)
    }
  }
}
