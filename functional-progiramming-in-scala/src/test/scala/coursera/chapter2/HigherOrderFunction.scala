package coursera.chapter2

import org.scalatest._

class HigherOrderFunctionTest extends FlatSpec with Matchers {

  "sum 1 to 10" should "be eql 55" in {
    def sumId(a: Int, b: Int) = HigherOrderFunction.TailRecursiveSum(x => x, a, b)
    assert(sumId(1, 10) == 55)
  }

  "square(3 + 4 + 5)" should "be eql 50" in {
    assert(HigherOrderFunction.TailRecursiveSum(x => x * x, 3, 5) == 50)
  }
}
