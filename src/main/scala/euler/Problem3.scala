package euler.Problem3

object Problem3 extends App {

  def getFirstPrime(n: Long): Long = {
    val limit = n / 2 // caching the limit of recursion-count

    def getFirstPrimeRecur(f: Long): Long = f match {
      case f if (f > limit) => n
      case f if (n % f == 0) => f
      case _ => getFirstPrimeRecur(f + 1)
    }

    getFirstPrimeRecur(2)
  }

  def isPrime(n: Long): Boolean = getFirstPrime(n) match {
    case k if k == n => true
    case _ => false
  }

  // by getting the first prime of divided number
  // we can reduce the number of computation 
  def getPrimes(n: Long): List[Long] = getFirstPrime(n) match {
      case k if k == n => List(n)
      case k => k :: getPrimes(n / k)
  }
}
