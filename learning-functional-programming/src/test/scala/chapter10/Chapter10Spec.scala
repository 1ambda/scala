package chapter10

import org.scalatest.{Matchers, WordSpec}
import Monoid._

class Chapter10Spec extends WordSpec with Matchers {
  import WordCount._

  "concatenate" in {
    concatenate(List("1", "2", "3", "4")) shouldBe "1234"
  }

  "foldMap" in {
    foldMap(List(1, 2, 3, 4))(_.toString)(stringMonoid) shouldBe "1234"
  }

  "foldRight" in {
    // custom impl
    foldRight(List(1, 2, 3, 4))("")(_ + _) shouldBe "1234"

    // scala standard foldRight
    List(1, 2, 3, 4).foldRight("")(_ + _) shouldBe "1234"
  }

  "foldLeft" in {
    // custom impl
    foldLeft(List(1, 2, 3, 4))(0)(_ - _) shouldBe -10
    foldRight(List(1, 2, 3, 4))(0)(_ - _) shouldBe -2

    // scala standard foldRight
    List(1, 2, 3, 4).foldLeft(0)((a, b) => a - b) shouldBe -10
    List(1, 2, 3, 4).foldRight(0)((a, b) => a - b) shouldBe -2
  }

  "foldMapV" in {
    foldMapV(IndexedSeq(1, 2, 3, 4))(stringMonoid)(_.toString) shouldBe "1234"
  }

  "Monoid[WordCount].op" in {
    val wc1 = Stub("")
    val wc2 = Part("", 3, "papfq")
    val wc3 = Part("asdasd", 0, "")
    val zero = wordCountMonoid.zero

    wordCountMonoid.op(wordCountMonoid.op(wc1, wc2), wc3) shouldBe
      wordCountMonoid.op(wc1, wordCountMonoid.op(wc2, wc3))
  }

  "Monoid[WordCount].zero" in {
    val wc1 = Stub("")
    val wc2 = Part("", 3, "papfq")
    val wc3 = Part("asdasd", 0, "")
    val zero = wordCountMonoid.zero

    wordCountMonoid.op(wc1, zero) shouldBe wordCountMonoid.op(zero, wc1)
    wordCountMonoid.op(wc2, zero) shouldBe wordCountMonoid.op(zero, wc2)
    wordCountMonoid.op(wc3, zero) shouldBe wordCountMonoid.op(zero, wc3)
  }

  "WordCountMonoid.count" in {
    val s1 = ""
    val s2 = " 1"
    val s3 = "asd asd"
    val s4 = " 2 "

    count(s1) shouldBe 0
    count(s2) shouldBe 1
    count(s3) shouldBe 2
    count(s4) shouldBe 1
  }
}
