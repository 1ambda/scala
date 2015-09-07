package chapter10

import org.scalatest.{Matchers, WordSpec}
import Monoid._

class Chapter10Spec extends WordSpec with Matchers {

  "concatenate" in {
    concatenate(List("1", "2", "3", "4")) shouldBe "1234"
  }

  "foldMap" in {
    foldMap(List(1, 2, 3, 4))(_.toString) shouldBe "1234"
  }
}
