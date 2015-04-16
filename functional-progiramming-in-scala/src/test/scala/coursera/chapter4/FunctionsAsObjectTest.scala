package coursera.chapter4

import org.scalatest._

class FunctionsAsObjectTest extends FlatSpec with Matchers {

  "List()" should "create an empty list" in {
    val e = List()
    assert(e.isEmpty == true)
  }

  "List(3)" should "contain 3 as an element" in {
    val e3 = List(3)
    assert(e3.head == 3)
  }

  "List(3, 4)" should "contain 3 as an element" in {
    val e34 = List(3, 4)
    assert(e34.tail.head == 4)
  }

  "List[NonEmpty].prepend(Empty)" should "pass compile test" in {
    def f(xs: List[NonEmpty]) = xs prepend Empty
  }
}
