package functor

import org.scalatest.{Matchers, FlatSpec}

import scalaz.Functor
import scalaz.std.option._
import scalaz.syntax.functor._
import scalaz.std.list._

class FunctorSpec extends FlatSpec with Matchers {

  val len: String => Int = _.length

  "A Option" should "be a functor" in {
    Functor[Option].map(Some("asdf"))(len) shouldEqual Some(4)
    Functor[Option].map(None)(len) shouldEqual None
  }

  "A List" should "be a functor" in {
    Functor[List].map(List("qwer", "asdfg"))(len) shouldBe List(4, 5)
  }

  "Functor" can "be used with lift" in {
    val lenOption: Option[String] => Option[Int] = Functor[Option].lift(len)

    lenOption(Some("abcd")) shouldBe Some(4)
  }

  "Functor" should "provide strengthL, R functions" in {
    Functor[List].strengthL("a", List(1, 2, 3)) shouldBe List("a" -> 1, "a" -> 2, "a" -> 3)
    Functor[List].strengthR(List(1, 2, 3), "a") shouldBe List(1 -> "a", 2 -> "a", 3 -> "a")

    List(1, 2, 3).strengthL("a") shouldBe List("a" -> 1, "a" -> 2, "a" -> 3)
    List(1, 2, 3).strengthR("a") shouldBe List(1 -> "a", 2 -> "a", 3 -> "a")
  }
}
