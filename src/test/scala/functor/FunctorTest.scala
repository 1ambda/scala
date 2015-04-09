package functor

import org.scalatest._
import scalaz._
import Scalaz._

class FunctorTest extends FlatSpec with Matchers {

  "List" should "be functor" in  {
    val expected1 = List(1, 2, 3, 4) map {_ + 1}
    expected1 should be (List(2, 3, 4, 5))

    val expected2 = List(1, 2, 3) map {3*}
    expected2 should be (List(3, 6, 9))
  }

  "Tuple" should "be functor. but f must be applied to the last member of the tuple" in {
    val expected = (1, 2, 3) map {_ * 2}
    expected should be ((1, 2, 6))
  }

  "Function" should "be functor" in  {
    val f = ((x: Int) => x + 1)
    val g = f map {_ * 7}

    val expected = g(3)

    expected should be ((3 + 1) * 7)
  }

  "Lifting" should "be return a function which can handle functors" in  {
    val lifted = Functor[List].lift { (_: Int) * 2 }

    val expected = lifted(List(3))

    expected should be (List(6))
  }
}
