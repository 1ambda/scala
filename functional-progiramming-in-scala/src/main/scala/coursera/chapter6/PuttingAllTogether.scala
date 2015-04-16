package coursera.chapter6

import scala.io._

object PuttinAllToghther extends App {

  val path = "/home/anster/github/coursera-scala/src/main/scala/coursera/chapter6/linux.words"
  val in = Source.fromFile(path)
  // java's iterator doesn't have groupBy
  val words = in.getLines.toList filter { _ forall { _.isLetter }}

  val mnem = Map(
    '2' -> "ABC",
    '3' -> "DEF",
    '4' -> "GHI",
    '5' -> "JKL",
    '6' -> "MNO",
    '7' -> "PQRS",
    '8' -> "TUV",
    '9' -> "WXYZ")

  // A to Z -> 2 to 9
  val charCode = mnem flatMap { case(k, v) => v map { c => (c, k) } }
  // or for ((digit, str) <- mnem; ltr <- str) yield ltr -> digit

  // "Java" -> "5282"
  def wordCode(word: String): String = word.toUpperCase map charCode

  // "5282" -> List("Java", "Kava", ...), "1111" -> List()
  val wordsForNum: Map[String, Seq[String]] =
    words groupBy wordCode withDefaultValue Seq()

  // return all ways to encode a number as a list of words
  def encode(number: String): Set[List[String]] =
    if (number.isEmpty) Set(List())
    else {
      (for {
        split <- 1 to number.length
        word <- wordsForNum(number take split)
        rest <- encode(number drop split)
      } yield word :: rest).toSet
    }

  def printEncoded(number: String) = {
    encode(number) map { _ mkString " "} map println
  }

  def translate(number: String): Set[String] = {
    encode(number) map { _ mkString " " }
  }

  translate("7225247386") foreach { println _ }
}
