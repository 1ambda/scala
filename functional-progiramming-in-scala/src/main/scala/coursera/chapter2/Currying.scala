package coursera.chapter2

import math.abs

object Currying {

  def sum(f: Int => Int): (Int, Int) => Int = {

    def sumF(a: Int, b: Int): Int = {
      def loop(a: Int, acc: Int): Int = {
        if (a > b) acc
        else loop(a + 1, f(a) +  acc)
      }

      loop(a, 0)
    }

    sumF
  }

  def sum1(f: Int => Int): (Int, Int) => Int = {
    def sumF(a: Int, b: Int): Int = {
      if (a > b) 0
      else f(a) + sumF(a+1, b)
    }

    sumF
  }

  def sum2(f: Int => Int)(a: Int, b: Int): Int = {
    if (a > b) 0
    else f(a) + sum2(f)(a+1, b)
  }

  def product(f: Int => Int)(a: Int, b: Int): Int = {
    if (a > b) 1
    else f(a) * product(f)(a + 1, b)
  }

  def factorial(n: Int): Int = {
    product(x => x)(1, n)
  }

  def mapReduce(f: Int => Int, combine: (Int, Int) => Int, init: Int)(a: Int, b: Int): Int = {
    if (a > b) init
    else combine(f(a), mapReduce(f, combine, init)(a + 1, b))
  }

  def sumUsingMapReduce(f: Int => Int)(a: Int, b: Int) =
    mapReduce(f, (x: Int, y: Int) => x + y, 0)(a, b)

  def productUsingMapReduce(f: Int => Int)(a: Int, b: Int) =
    mapReduce(f, (x: Int, y: Int) => x * y, 1)(a, b)

  val tolerance = 0.0001 // = 1.0E-4
  def isCloseEnough(x: Double, y: Double) = {
    abs((x - y) / x) / x < tolerance
  }

  def fixedPoint(f: Double => Double)(firstGuess: Double): Double = {
    def iterate(guess: Double): Double = {
      val next = f(guess)
      if (isCloseEnough(guess, next)) next
      else iterate(next)
    }
    iterate(firstGuess)
  }

  def avgDamp(f: Double => Double)(x: Double) = (x + f(x)) / 2
  def sqrt(x: Double): Double = fixedPoint(avgDamp(y => x / y))(1)
}
