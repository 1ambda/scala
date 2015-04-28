package calculator

import math.sqrt

object Polynomial {
  def computeDelta(a: Signal[Double], b: Signal[Double],
      c: Signal[Double]): Signal[Double] = {
    Signal {
      val A = a()
      val B = b()
      val C = c()

      // Δ = b² - 4ac
      B * B - 4 * A * C
    }
  }

  def computeSolutions(a: Signal[Double], b: Signal[Double],
                       c: Signal[Double], delta: Signal[Double]): Signal[Set[Double]] = {
    Signal {
      val B = b()
      val A = a()
      val D = delta()

      // (-b ± √Δ) / (2a)
      val first = if (D < 0) 0 else (-B + sqrt(D)) / 2 * A
      val second = if (D < 0) 0 else (-B - sqrt(D)) / 2 * A

      Set(first, second)
    }
  }
}
