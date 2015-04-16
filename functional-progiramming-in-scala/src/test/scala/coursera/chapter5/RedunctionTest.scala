package coursera.chapter5

import org.scalatest._

class RedunctionTest extends FlatSpec with Matchers {

  import Redunction._

  val xs = 1 to 10 toList
  val ys = List(1 to 5: _*)
  "sum(1 to 10)" should "return 55" in {
    assert(sum1(xs) == 55)
    assert(sum2(xs) == 55)
  }

  "product(1 to 5)" should "return 120" in {
    assert(product1(ys) == 120)
    assert(product2(ys) == 120)
  }

  "lengthFun" can "calculate the length of a list" in {
    assert(lengthFun(xs) == 10)
  }

  "mapFun" must "return the f applied list" in {
    val ks = (2 to 20 by 2).toList

    assert(mapFun(xs, (x: Int) => x * 2) == ks)
  }
}
