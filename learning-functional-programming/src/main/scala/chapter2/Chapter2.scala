package chapter2

object Chapter2 {
  def factorial(n: Int): Int = {
    @annotation.tailrec
    def recur(n: Int, acc: Int): Int = {
      if (n == 0) acc
      else recur(n - 1, acc * n)
    }

    recur(n, 1)
  }

  def abs(n: Int) = if (n < 0) -n else n

  def format(message: String, n: Int, f: Int => Int): String = {
    s"$message${f(n)}"
  }

  def formatAbs(n: Int) = format(s"abs($n): ", n, abs)
  def formatFactorial(n: Int) = format(s"factorial(${n}): ", n, factorial)
}
