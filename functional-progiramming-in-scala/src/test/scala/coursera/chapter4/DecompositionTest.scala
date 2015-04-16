package coursera.chapter4

import org.scalatest._

class DecompositionTest extends FlatSpec with Matchers {

  import Decomposition._

  val three = Number(3)
  val four = Number(4)
  val seven = Number(7)
  val sum = Sum(three, four)
  val x = Var("x")
  val y = Var("y")
  val z = Var("z")

  "Sum(Number(3), Number(4))" should "be eql 7" in {
    assert(sum.eval == 7)
  }

  "show(Sum(Number(3), Number(4)))" should "be 3 + 4" in {
    assert(show(sum) == "3 + 4")
  }

  "show(3+(4+7))" should "be 3+4+7" in {
    assert(show(Sum(three, Sum(four, seven))) == "3 + 4 + 7")
  }

  "show(x + y)" should "be x + y" in {
    assert(show(Sum(x, y)) == "x + y")
  }

  "show(x * y)" should "be x * y" in {
    assert(show(Prod(x, y)) == "x * y")
  }

  "show(x * (x + z))" should "be x * (y + z)" in {
    assert(show(Prod(x, Sum(y, z))) == "x * (y + z)")
    assert(show(Sum(Prod(x, y), z)) == "x * y + z")
  }
}
