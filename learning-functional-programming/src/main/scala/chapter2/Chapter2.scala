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


  def findFirst[A](p: A => Boolean, xs: List[A]): Int = {
    @annotation.tailrec
    def loop(n: Int): Int = {
      if (xs.length < n) -1
      else if (p(xs(n))) n
      else loop(n + 1)
    }

    loop(0)
  }

  def isSorted[A](arr: Array[A], ordered: (A, A) => Boolean): Boolean = {
    @annotation.tailrec
    def loop(n: Int): Boolean = {
      if (n >= arr.length) true
      else if (ordered(arr(n), arr(n+1))) loop(n+1)
      else false
    }

    loop(0)
  }

  def lessThan1(x: Int, y: Int): Boolean = x < y
  val lessThan2 = new Function2[Int, Int, Boolean] {
    override def apply(x: Int, y: Int) = x < y
  }

  def partial1[A, B, C](a: A, f: (A, B) => C): B => C =
    (b: B) => f(a, b)

  def curry[A, B, C](f: (A, B) => C): A => (B => C) =
    (a: A) => (b: B) => f(a, b)

  def uncurry[A, B, C](f: A => B => C): (A, B) => C =
    (a: A, b: B) => f(a)(b)

  def compose[A, B, C](f: B => C, g: A => B): A => C =
    g andThen f
  /* (a: A) => f(g(a)) */
}
