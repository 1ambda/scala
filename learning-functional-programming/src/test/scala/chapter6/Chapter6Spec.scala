package chapter6

import org.scalatest.{Matchers, FunSuite}

class Chapter6Spec extends FunSuite with Matchers {
  test("SimpleRNG test") {
    val rng = SimpleRNG(42)

    val (n1, rng1) = rng.nextInt
    val (n2, rng2) = rng.nextInt
    val (n3, rng3) = rng2.nextInt

    n1 shouldBe n2
    n1 should not be n3
  }

  test("nonNegativeInt test") {
    val rng1 = SimpleRNG(42)

    val (n1, rng2) = rng1.nonNegativeInt(rng1)
    val (n2, rng3) = rng2.nonNegativeInt(rng2)
    val (n3, rng4) = rng3.nonNegativeInt(rng3)

    n1 should be >= 0
    n2 should be >= 0
    n3 should be >= 0
  }

}
