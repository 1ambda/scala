package coursera.chapter7

import org.scalatest._

class Chapter7Test extends FlatSpec with Matchers {

  import Chapter7._

  "streamRange(1, 2)" should "return Stream(1, ?)" in {
    val s12 = streamRange(1, 2)
    assert(s12 == streamRange(1, 2))

    // streamRange(1, 10).take(3).toList
    // expr
    // val primes = sieve(from(2)).take(100).toList
    // println(primes)
    // val a = sqrtStream(4).take(10).toList
    // println(a)
    val b = sqrtStream(4).filter(isGoodEnough(_, 4)).take(10).toList
    println(b)
  }

}
