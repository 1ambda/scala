package calculator

import org.scalatest.FunSuite

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import org.scalatest._

import TweetLength.MaxTweetLength

@RunWith(classOf[JUnitRunner])
class CalculatorSuite extends FunSuite with ShouldMatchers {

  /******************
    ** TWEET LENGTH **
    ******************/

  def tweetLength(text: String): Int =
    text.codePointCount(0, text.length)

  test("tweetRemainingCharsCount with a constant signal") {
    val result = TweetLength.tweetRemainingCharsCount(Var("hello world"))
    assert(result() == MaxTweetLength - tweetLength("hello world"))

    val tooLong = "foo" * 200
    val result2 = TweetLength.tweetRemainingCharsCount(Var(tooLong))
    assert(result2() == MaxTweetLength - tweetLength(tooLong))
  }

  test("tweetRemainingCharsCount with a supplementary char") {
    val result = TweetLength.tweetRemainingCharsCount(Var("foo blabla \uD83D\uDCA9 bar"))
    assert(result() == MaxTweetLength - tweetLength("foo blabla \uD83D\uDCA9 bar"))
  }


  test("colorForRemainingCharsCount with a constant signal") {
    val resultGreen1 = TweetLength.colorForRemainingCharsCount(Var(52))
    assert(resultGreen1() == "green")
    val resultGreen2 = TweetLength.colorForRemainingCharsCount(Var(15))
    assert(resultGreen2() == "green")

    val resultOrange1 = TweetLength.colorForRemainingCharsCount(Var(12))
    assert(resultOrange1() == "orange")
    val resultOrange2 = TweetLength.colorForRemainingCharsCount(Var(0))
    assert(resultOrange2() == "orange")

    val resultRed1 = TweetLength.colorForRemainingCharsCount(Var(-1))
    assert(resultRed1() == "red")
    val resultRed2 = TweetLength.colorForRemainingCharsCount(Var(-5))
    assert(resultRed2() == "red")
  }

  /******************
    ** CALCULATOR  **
    ******************/

  import Calculator._

  val refs: Map[String, Signal[Expr]] = Map(
    "a" -> Signal(Literal(1.5)),
    "b" -> Signal(Literal(-0.2)),
    "c" -> Signal(Ref("b")),
    "d" -> Signal(Plus(Ref("a"), Ref("b"))),
    "e" -> Signal(Ref("f")),
    "f" -> Signal(Plus(Ref("c"), Ref("e")))
  )

  test("calculator: getReferenceExpr test") {
    assert(getReferenceExpr("k", refs).toString == "Literal(NaN)")
    assert(getReferenceExpr("a", refs) == Literal(1.5))
  }

  test("calculator: eval") {
    val e1 = getReferenceExpr("a", refs)
    assert(eval(e1, refs) == 1.5)

    val e2 = getReferenceExpr("c", refs)
    assert(eval(e2, refs) == -0.2)

    // Plus
    val e3 = getReferenceExpr("d", refs)
    assert(eval(e3, refs) == 1.3)

    // Times
    val e4 = Times(Literal(-1.5), Literal(2))
    val e5 = Times(Literal(1.5), Literal(0))
    assert(eval(e4, refs) == -3.0)
    assert(eval(e5, refs) == 0.0)

    // Divide
    val e6 = Divide(Literal(-1.5), Literal(2))
    val e7 = Divide(Literal(1.5), Literal(0))
    val e8 = Divide(Literal(-1.5), Literal(0))
    val e9 = Divide(Literal(0), Literal(2))
    assert(eval(e6, refs) == -0.75)
    assert(eval(e7, refs) == Double.PositiveInfinity)
    assert(eval(e8, refs) == Double.NegativeInfinity)
    assert(eval(e9, refs) == 0.0)
  }

  test("calculator: prevent cyclic definition") {
    println(getReferenceExpr("f", refs))
  }
}















