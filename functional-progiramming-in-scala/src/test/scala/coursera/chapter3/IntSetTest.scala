package coursera.chapter3

/*
   This is a stub test class. To learn how to customize it,
see the documentation for `ensime-goto-test-configs'
*/

import org.scalatest._

class IntSetTest extends FlatSpec with Matchers {

  "EmptySet" should "not have element" in {
    assert(!EmptySet.contains(3))
  }

  "{.3{.4.}}" should "incl 3 and 4" in {
    val t1 = EmptySet.incl(3)
    val t2 = t1.incl(4)

    assert(t1.contains(3))
    assert(t2.contains(3))
    assert(t2.contains(4))
  }

  "{.3{.4.}} union {.5.}" should "incl 3, 4 and 5" in {
    val t1 = EmptySet incl 3 incl 4
    val t2 = EmptySet incl 5
    val t3 = t1 union t2

    assert(t3.contains(3))
    assert(t3.contains(4))
    assert(t3.contains(5))
  }

}
