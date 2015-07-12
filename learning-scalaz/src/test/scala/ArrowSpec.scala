import org.scalatest.{FunSuite, Matchers}



/*
  ref: http://eed3si9n.com/learning-scalaz-day15

  An arrow is the term used in category theory as an abstract notion of thing
  that behaves like a function. In Scalaz, these are

  - Function1[A, B]
  - PartialFunction[A, B]
  - Kleisli[F[_], A, B]
  - CoKelisli[F[_], A,, B].Arrow

 */

class ArrowSpec extends FunSuite with Matchers {

  import scalaz._
  import Scalaz._
  import Kleisli._

  test("Arrow example") {
    val plus1 = (_: Int) + 1
    var times2 = (_: Int) * 2
    val rev = (_: String) reverse

    plus1.first apply (7, "abc") shouldBe (8, "abc")
    plus1.second apply ("def", 14) shouldBe ("def", 15)
    (plus1 *** rev)(8, "abc") shouldBe (9, "cba")
    (plus1 &&& times2)(7) shouldBe (8, 14)
    plus1.product (9, 99) shouldBe (10, 100)
  }

  test("Arrow.>>>, Arrow.<<<") {
    val f = (_: Int) + 1
    val g = (_: Int) * 100

    (f >>> g)(2) shouldBe 300
    (f <<< g)(2) shouldBe 201
  }

  test("Arrow.***, Arrow.&&&") {
    val f = (_: Int) + 1
    val g = (_: Int) * 100

    // (***) combines two arrows into a new arrow by running the arrows on a pair of values
    (f *** g)(1, 2) shouldBe (2, 200)

    // (&&&) combines two arrows into a new arrow by running the two arrows on the same value
    (f &&& g)(2) shouldBe (3, 200)

    // see, https://wiki.haskell.org/Arrow_tutorial
  }

  // ref: http://www.casualmiracles.com/2012/07/02/a-small-example-of-kleisli-arrows/
  test("KleisliArrow") {
    def optStr(x: Int): Option[String] = x.toString.some
    def optInt(x: String): Option[Int] = x.toInt.some
    def double(x: Int): Option[Double] = (x * 2.0).some

    def composeOldway(i: Int) = for {
      x <- optStr(i)
      y <- optInt(x)
      z <- double(y)
    } yield z

    // doesn't compile
    // val funky = kleisli(optStr _) >=> (optInt _) >=> (double _)
  }
}
