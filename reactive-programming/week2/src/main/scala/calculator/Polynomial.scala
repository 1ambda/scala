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
      var solutions: Set[Double] = Set()

      // (-b ± √Δ) / (2a)
      if (delta() >= 0) solutions += (-b() + sqrt(delta())) / (2 * a())
      if (delta() >= 0) solutions += (-b() - sqrt(delta())) / (2 * a())

      solutions
    }
  }
}
