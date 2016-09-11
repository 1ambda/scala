package io.github.lambda.week3

import scala.collection.GenSet

object DataParallelOperations extends App {
  def parallelSum(xs: Array[Int]): Int = {
    xs.par.foldLeft(0)(_ + _)
  }

  def parallelMax(xs: Array[Int]): Int = {
    xs.par.fold(Int.MinValue)(math.max)
  }

  def play(a: String, b: String): String = List(a, b).sorted match {
    case List("paper", "scissors") => "scissors"
    case List("paper", "rock")     => "scissors"
    case List("rock",  "scissors") => "scissors"
    case List(a, b) if a == b      => a
    case List("", b)               => b
  }

  // println(Array("paper", "rock", "paper", "scissors").par.fold("")(play))

  def isVowel(c: Char) = "AEIOUaeiou".contains(c)

  def aggregateChars(cs: Array[Char]): Int = {
    cs.par.aggregate(0)({ (count, c) =>
      if (isVowel(c)) count + 1
      else count
    }, _ + _)
  }

  // println(aggregateChars("EPFL".toList.toArray))

}
