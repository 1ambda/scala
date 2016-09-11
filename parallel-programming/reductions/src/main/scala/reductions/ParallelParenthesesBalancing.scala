package reductions

import scala.annotation._
import org.scalameter._
import common._

object ParallelParenthesesBalancingRunner {

  @volatile var seqResult = false

  @volatile var parResult = false

  val standardConfig = config(
    Key.exec.minWarmupRuns -> 40,
    Key.exec.maxWarmupRuns -> 80,
    Key.exec.benchRuns -> 120,
    Key.verbose -> true
  ) withWarmer(new Warmer.Default)

  def main(args: Array[String]): Unit = {
    val length = 100000000
    val chars = new Array[Char](length)
    val threshold = 10000
    val seqtime = standardConfig measure {
      seqResult = ParallelParenthesesBalancing.balance(chars)
    }
    println(s"sequential result = $seqResult")
    println(s"sequential balancing time: $seqtime ms")

    val fjtime = standardConfig measure {
      parResult = ParallelParenthesesBalancing.parBalance(chars, threshold)
    }
    println(s"parallel result = $parResult")
    println(s"parallel balancing time: $fjtime ms")
    println(s"speedup: ${seqtime / fjtime}")
  }
}

object ParallelParenthesesBalancing {

  /** Returns `true` iff the parentheses in the input `chars` are balanced.
   */
  def balance(chars: Array[Char]): Boolean = {
    var pSum = 0
    var index = 0

    while (index < chars.length) {
      val c: Char = chars(index)

      if (c == '(') pSum += 1
      else if (c == ')') pSum -= 1

      if (pSum < 0) index = chars.length /** break out */

      index += 1
    }

    pSum == 0
  }

  /** Returns `true` iff the parentheses in the input `chars` are balanced.
   */
  def parBalance(chars: Array[Char], threshold: Int): Boolean = {

    def traverse(idx: Int, until: Int, arg1: Int, arg2: Int): (Int, Int) = {
      var minParenSum = 0
      var totalParenSum = 0
      var index = idx

      while(index < until) {
        val c = chars(index)

        if (c == '(') totalParenSum += 1
        else if (c == ')') totalParenSum -= 1

        if (totalParenSum < minParenSum) minParenSum = totalParenSum

        index += 1
      }

      (minParenSum, totalParenSum)
    }

    def reduce(from: Int, until: Int): (Int, Int) = {
      if (until - from <= threshold) return traverse(from, until, 0, 0)

      val middle = from + (until - from) / 2

      val (left, right) = parallel(reduce(from, middle), reduce(middle, until))

      /** (min, sum) */
      (Math.min(left._1, left._2 + right._1), left._2 + right._2)
    }

    reduce(0, chars.length) == (0, 0)
  }

  // For those who want more:
  // Prove that your reduction operator is associative!

}
