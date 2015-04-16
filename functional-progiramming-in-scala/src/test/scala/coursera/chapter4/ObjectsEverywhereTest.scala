
package coursera.chapter4

import org.scalatest._

class ObjectsEverywhereTest extends FlatSpec with Matchers {

  "!False" should "be True" in {
    Assert(!False == True)
  }

  "False && True" should "be False" in {
    Assert(!(False && True))
  }

  val one = Zero.successor
  val two = one + one
  val three = two + one
  val four = three + one

  "One + One" should "be Two" in {
    assert(two.predecessor.predecessor == Zero)
    assert(two.number == 2)
  }

  "two + two" should "be four" in {
    assert(four.predecessor.predecessor.predecessor.predecessor == Zero)
    assert(four.number == 4)
  }

  "One - One" should "be Zero" in {
    assert((one - one) == Zero)
  }

  "Two - Zero" should "be Two" in {
    assert((two - Zero).predecessor.predecessor == Zero)
  }
}
