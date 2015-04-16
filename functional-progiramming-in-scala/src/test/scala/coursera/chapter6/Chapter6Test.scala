package coursera.chapter6

import org.scalatest._

/*
   This is a stub test class. To learn how to customize it,
see the documentation for `ensime-goto-test-configs'
*/

class Chater6Test extends FlatSpec with Matchers {

  import Chapter6._

  "isPrime(2)" should "be true" in {
    assert(isPrime(37))
    assert(isPrime(38) == false)
  }

  "getPrimeParis(4)" should "return (2, 1), (3, 2)" in {
    assert(getPrimePairs(4) == Vector((2, 1), (3, 2)))
  }

  "getPrimeParis2(4)" should "return (2, 1), (3, 2)" in {
    assert(getPrimePairs2(4) == Vector((2, 1), (3, 2)))
  }

  "scalaProduct(List(1, 2, 3), List(1, 2, 3))" should "return 14.0" in {
    val a = List(1.0, 2.0, 3.0)
    val b = List(1.0, 2.0, 3.0)

    assert(scalaProduct(a, b) == 14.0)
  }

  "nQueens" should "exist" in {
    // println(nQueens(4) map showQueens)
  }

  "Poly" should "be printed" in {
    println(new Poly(0 -> 0.3, 1 -> 0, 2 -> 3))
  }

  "(x^2 + 3x) + (-2x + 7)" should "be x^2 + x + 7" in {
    val p1 = new Poly(2->1, 1->3)
    val p2 = new Poly(1->(-2), 0->7)
    val p3 = new Poly(2->1, 1->1, 0->7)

    assert((p1 + p2).terms == p3.terms)
  }
}
