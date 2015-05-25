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

  test("findFirst") {
    val ns = List(1, 2, 3, 4)
    val ss = List('a', 'b', 'c', 'd')

    findFirst((x: Int) => x == 2  , ns) should be (1)
    findFirst((c: Char) => c == 'd', ss) should be (3)
  }

  test("isSorted") {
    val ws = Array(1, 2, 4, 3)
    val xs = Array(4, 3, 2, 1)
    val ys = Array(0, 0, 1, -1)
    val zs = Array(-1, 2, 0)

    val fixtures = List(ws, xs, ys, zs)
    val pred = (x: Int, y: Int) => x < y

    val result = fixtures.map(isSorted(_, pred))
    result should not contain (true)
  }

  test("lessThan") {
    lessThan1(1, 3) should be (lessThan2(1, 3))
  }
}
