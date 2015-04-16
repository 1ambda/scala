package coursera.chapter5

import org.scalatest._

class HigherOrderFunctionTest extends FlatSpec with Matchers {

  import HigherOrderFunction._

  val xs = List(1, 2, 3, 4)
  val ys = List(1, 4, 9, 16)

  "squareList1" should "square each element" in {
    assert(squareList1(xs) == ys)
  }

  "squareList2" should "square each element" in {
    assert(squareList2(xs) == ys)
  }

  val zs = List(2, -4, 1, 5, 7)
  val ts = List("a", "a", "b", "c", "c", "a")
  val ss = List(List("a", "a"), List("b"), List("c", "c"), List("a"))

  "pack" should "return a packed list" in {
    assert(pack(ts) == ss)
  }
}
