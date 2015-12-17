package tutorial

import util.TestSuite


/**
 * ref - https://github.com/milessabin/shapeless/wiki/Feature-overview%3A-shapeless-2.0.0
 */


class ShapelessTutorialSpec extends TestSuite {
  test("Polymorphic function values") {

    import shapeless._, poly._

    object choose extends (Set ~> Option) {
      def apply[T](s: Set[T]) = s.headOption
    }

    val s1 = Set(1, 2, 3)
    val s2 = Set('a', 'b', 'c')

    choose(Set(1, 2, 3)) shouldBe Some(1)
    choose(Set('a', 'b', 'c')) shouldBe Some('a')

    def pairApply[A, B](f: Set ~> Option)(s1: Set[A])(s2: Set[B]) =
      (f(s1), f(s2))

    pairApply(choose)(s1)(s2) shouldBe (Some(1), Some('a'))

    // interoperable with ordinary mono-morphic function values
    List(Set(1, 3, 5), Set(2, 4, 6)) map choose shouldBe List(Some(1), Some(2))

    // capturing type specific cases
    object size extends Poly1 {
      implicit def caseInt = at[Int](x => 1)
      implicit def caseString = at[String](_.length)
      implicit def caseTuple[T, U]
      (implicit st: Case.Aux[T, Int], su: Case.Aux[U, Int]) = at[(T, U)](tu =>
        size(tu._1) + size(tu._2)
      )
    }

    size(23) shouldBe 1
    size("foo") shouldBe 3
    size((23, "foo")) shouldBe 4
    size(((23, "foo"), 13)) shouldBe 5
  }
}
