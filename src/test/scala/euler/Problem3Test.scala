package euler.Problem3

import org.scalatest._

class Problem3Test extends FlatSpec with Matchers {

  import Problem3._

  "isPrime 6008514751431" should "be false" in {
    assert(isPrime(2))
    assert(isPrime(3))
    assert(isPrime(4) == false)
    assert(isPrime(13195) == false)
    assert(isPrime(600851475143l) == false)
  }

  "isPrime 151101047" should "be true" in {
    // http://www.prime-numbers.org/prime-number-151100000-151105000.htm
    assert(isPrime(151101047))
  }

  "getfirstprime(4)" should "return 2" in {
    assert(getFirstPrime(4) == 2)
  }

  "getPrimes(4)" should "return List(2,2)" in {
    assert(getPrimes(4) == List(2, 2))
  }

  "getPrimes(13195)" should "return List(5, 7, 13, 29)" in {
    assert(getPrimes(13195) == List(5, 7, 13, 29))
  }

  "The Answer of Problem 3" must "be 6857" in {
    assert((getPrimes(600851475143l).max) == 6857l)
  }
}
