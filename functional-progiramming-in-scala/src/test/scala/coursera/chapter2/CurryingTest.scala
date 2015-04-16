package coursera.chapter2

import org.scalatest._

class CurryingTest extends FlatSpec with Matchers {

  "sumInts(1, 10)" should "return 55" in {
    def sumInts = Currying.sum(x => x)
    assert(sumInts(1, 10) == 55)
  }
  
  "sum1Ints(1, 10)" should "return 55" in {
    def sum1Ints = Currying.sum1(x => x)
    assert(sum1Ints(1, 10) == 55)
  }

  "sum2(x => x)(1, 10)" should "return 55" in {
    assert(Currying.sum2(x => x)(1, 10) == 55)
  }

  "product(x => x * x)(3, 4)" should "return 144" in{
    assert(Currying.product(x => x * x)(3, 4) == 144)
  }

  "factorial(4)" should "return 24" in {
    assert(Currying.factorial(4) == 24)
  }


  "sumUsingMapReduce(x => x)(1, 10)" should "return 55" in {
    assert(Currying.sumUsingMapReduce(x => x)(1, 10) == 55)
  }

  "productUsingMapReduce(x => x * x)(3, 4)" should "return 144" in{
    assert(Currying.productUsingMapReduce(x => x * x)(3, 4) == 144)
  }
}
