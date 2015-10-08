package monocles

import org.scalatest._

class OptionalTutorial extends WordSpec with Matchers {
  
  import monocle._, Monocle._
 

  "at" in {
    def __at[K, V](key: K) = Lens[Map[K, V], Option[V]](m => m.get(key)) { optV => m =>
      optV match {
        case None    => m - key
        case Some(v) => m + (key -> v)
      }
    }

    val m1 = Map("one" -> 1, "two" -> 2)

    __at("two").set(None)(m1) shouldBe Map("one" -> 1)
    __at("three").set(Some(3))(m1) shouldBe m1 + ("three" -> 3)
  }

  /**
   * case class Optional[S, A] {
   *  getOption: S => Option[A],
   *  set:       (A, S) => S
   * }
   *
   * getOption(s) map set(_, s) == Some(s)
   * getOption(set(a, s)) == Some(a) | None
   *
   * def cons[A]: Prism[List[A], (A, List[A])]
   * def first[A, B]: Lens[(A, B), A]
   * def head[A]: Optional[List[A], A] = cons compose first
   *
   * def void[S, A]: Optional[S, A] = Optional(
   *  s => None,
   *  (a, s) => s)
   */

  "void" in {
    Optional.void.getOption("Hello") shouldBe None
    Optional.void.set(1)("Hello") shouldBe "Hello"
    Optional.void.setOption(1)("Hello") shouldBe None
  }


  /**
   * def index[A](i: Int): Optional[List[A], A] =
   *  if (i < 0) void
   *  else if (i == 0) head
   *  else cons compose second compose index(i - 1)
   */
  "index" in {
    val l = List(1, 2, 3)
    (l applyOptional index(-1) getOption) shouldBe None
    (l applyOptional index(1) getOption) shouldBe Some(2)
    (l applyOptional index(5) getOption) shouldBe None

    (l applyOptional index(1) set(10)) shouldBe List(10, 2, 3)
  }


}


