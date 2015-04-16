package coursera.chapter5

import org.scalatest._

class PairsAndTuplesTest extends FlatSpec with Matchers {

  import PairsAndTuples._

  "msort" should "return an ordered list" in {
    val xs = List(4, 2, 7, 1, 11, 9, 3)
    val ys = List(10, 9, 8, 7, 6)
    val zs = List("pineapple", "apple", "banana", "watermelon")

    assert(msort(xs) == List(1, 2, 3, 4, 7, 9, 11))
    assert(msort(ys) == List(6, 7, 8, 9, 10))
    assert(msort(zs) == List("apple", "banana", "pineapple", "watermelon"))
  }
}
