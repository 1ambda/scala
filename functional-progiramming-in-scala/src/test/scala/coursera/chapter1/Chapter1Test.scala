package coursera.chapter1

import org.scalatest._

class Chapter1Test extends FlatSpec with Matchers {
  "sqrt(4)" should "be 2 approximately" in {

    val sqrt4 = Math.sqrt(4);
    assert(2.0 <= sqrt4 && sqrt4 < 2.00005)
  }

  "gcd(3, 6) and gcd(6, 3)" should "return 3" in {
    assert(Math.gcd(3, 6) == 3)
    assert(Math.gcd(6, 3) == 3)
  }

  "factorial(4)" should "return 24" in {
    assert(Math.factorial(4) == 24)
  }

  "tailFactorial(4)" should "also return 24" in {
    assert(Math.tailFactorial(4) == 24)
  }
}
