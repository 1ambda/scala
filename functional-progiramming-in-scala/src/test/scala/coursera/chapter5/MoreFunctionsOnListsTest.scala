package coursera.chapter5

import org.scalatest._

class MoreFunctionsOnListsTest extends FlatSpec with Matchers {

  import MoreFunctionsOnLists._

  "init" should "return rest elements except for head" in {
    val xs = List(1, 2, 3, 4, 5)

    assert(init(xs) == List(1, 2, 3, 4))
  }

  "reverse" should "return the inversed list" in {
    val xs = List(1, 2, 3, 4)
    val ys = List(1, 2, 3, 4, 5)

    assert(reverse(xs) == List(4, 3, 2, 1))
    assert(reverse(ys) == List(5, 4, 3, 2, 1))
  }

  "removeAt" should "drop the n'th element" in {
    val xs = List(1, 2, 3, 4)
    assert(removeAt(2, xs) == List(1, 2, 4))
  }

  "flatten" should "make a flat list" in {
    val xs = List(1, 2, List(3, 4, List(5, List(6))))
    val ys = List(1, 2, 3, 4)
    assert(flatten(xs).length == 6)
    assert(flatten(ys) == List(1, 2, 3, 4))
    assert(flatten(xs) == List(1, 2, 3, 4, 5, 6))
  }
}
