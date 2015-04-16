package coursera.chapter1

object Math{
  def abs(x: Double) = if (x < 0) -x else x
  def sqrt(x: Int): Double = {

    def sqrtIter(guess: Double): Double =
      if (isGoodEnough(guess, x)) guess
      else sqrtIter(improve(guess))

    def isGoodEnough(guess: Double, x: Double): Boolean =
      abs(guess * guess - x) < 0.0001

    def improve(guess: Double) =
      (guess +  x / guess) / 2

    sqrtIter(1.0)
  }

  def gcd(a: Int, b: Int): Int =
    if (b == 0) a else gcd(b, a % b)

  def factorial(n: Int): Int =
    if (n == 0) 1 else n * factorial(n-1);

  def tailFactorial(n: Int) = {
    def loop(acc: Int, n: Int): Int =
      if (n == 0) acc else loop(acc * n, n-1)
    loop(1, n);
  }
}
