package coursera.chapter7

object Chapter7 {

  abstract class IntSet {
    def incl(x: Int): IntSet
    def contains(x: Int): Boolean
  }

  object Empty extends IntSet {
    def contains(x: Int): Boolean = false
    def incl(x: Int): IntSet = NonEmpty(x, Empty, Empty)
  }

  case class NonEmpty(elem: Int, left: IntSet, right: IntSet) extends IntSet {
    def contains(x: Int): Boolean =
      if (x < elem) left contains x
      else if (x > elem) right contains x
      else true

    def incl(x: Int): IntSet =
      if (x < elem) NonEmpty(elem, left incl x, right)
      else if (x > elem) NonEmpty(elem, left, right incl x)
      else this
  }

  def streamRange(l: Int, h: Int): Stream[Int] = {
    // println("l: " + l)
    if (l >= h) Stream.empty 
    else Stream.cons(l, streamRange(l + 1, h))
  }

  def expr = {
    val x = { println("x"); 1 }
    lazy val y = { println("y"); 2 }
    def z = { println("z"); 3 }
    z + y + x + z + y + x
  }

  def from(n: Int): Stream[Int] = n #:: from(n + 1)

  def sieve(s: Stream[Int]): Stream[Int] =
    s.head #:: sieve(s.tail filter (_ % s.head != 0))

  def sqrtStream(x: Double): Stream[Double] = {
    def improve(guess: Double) = (guess + x / guess) / 2
    lazy val guesses: Stream[Double] = 1 #:: (guesses map improve)
    guesses
  }

  def isGoodEnough(x: Double, guess: Double) =
    math.abs((guess * guess - x ) / x) < 0.0001
}
