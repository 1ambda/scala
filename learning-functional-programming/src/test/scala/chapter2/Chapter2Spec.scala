package chapter2

import org.scalatest.{Matchers, FunSuite}

class Chapter2Spec extends FunSuite with Matchers {
  import Chapter2._

  test("factorial(10) should be 3628800") {
    factorial(10) should be (3628800)
  }

  test("format") {
    formatFactorial(10) should be ("factorial(10): 3628800")
    formatAbs(-10) should be ("abs(-10): 10")
  }
}
