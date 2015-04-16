package coursera.chapter2

import math._

object FunctionsAndData {

}

class Rational(x: Int, y: Int) {
  require(y > 0, "denom != 0")

  // secondary constructor
  def this(x: Int) = this(x, 1)

  private def gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
  private val g = abs(gcd(x, y))

  def numer = x / g
  def denom = y / g

  def < (that: Rational) = numer * that.denom < that.numer * denom
  def max(that: Rational) = if (this < that) that else this

  def + (that: Rational) = {
    new Rational(
      numer * that.denom + that.numer * denom,
      denom * that.denom
    )
  }

  def - (that: Rational) = {
    this + -that
  }

  def unary_- = new Rational(-numer, denom)
}
