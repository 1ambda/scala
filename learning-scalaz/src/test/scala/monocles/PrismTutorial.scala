package monocles

import org.scalatest._
import monocle._, Monocle._, monocle.macros._

/* http://julien-truffaut.github.io/Monocle//tut/prism.html */
class PrismTutorial extends WordSpec with Matchers {

  import PrismTutorial._
  /**
   * A `Prism` is an Optic used to select part of a `Sum` type (also known as `Coproduct`)
   *
   * `Prism` have to type parameters `S` and `A` (`Prism[S, A]`) where
   * - `S` represents the `Sum`
   * - `A` a part of the `Sum`
   */

  val tuesday = GenPrism[Day, Tuesday.type] composeIso GenIso.unit[Tuesday.type]

  "Prism Basics" in {
    /* Since `Tuesday is a singleton, it is isomorphic to `Unit` */


    val _tuesday = Prism[Day, Unit] {
      case Tuesday => Some(())
      case _       => None
    }(_ => Tuesday)

    _tuesday.reverseGet(()) shouldBe Tuesday
    _tuesday.getOption(Monday) shouldBe None
    _tuesday.getOption(Tuesday) shouldBe Some(())

    tuesday.reverseGet(()) shouldBe Tuesday
    tuesday.getOption(Monday) shouldBe None
    tuesday.getOption(Tuesday) shouldBe Some(())
  }

  /* `LinkedList` is recursive data type that either empty or a cons */
  sealed trait LinkedList[A]
  case class LLNil[A]() extends LinkedList[A]
  case class LLCons[A](head: A , tail: LinkedList[A]) extends LinkedList[A]

  def _nil[A] = Prism[LinkedList[A], Unit] {
    case LLNil()      => Some(())
    case LLCons(_, _) => None
  }(_ => LLNil())

  def _cons[A] = Prism[LinkedList[A], (A, LinkedList[A])] {
    case LLCons(h, t) => Some((h, t))
    case LLNil()      => None
  } { case (h, t) => LLCons(h, t) }

  val l1 = LLCons(1, LLCons(2, LLCons(3, LLNil())))
  val l2 = _nil[Int].reverseGet(())

  "LinkedList with Prism" in {
    _cons.getOption(l1) shouldBe Some((1, LLCons(2, LLCons(3, LLNil()))))
    _cons.isMatching(l1) shouldBe true

    _cons[Int].modify(_.copy(_1 = 5))(l1) shouldBe LLCons(5, LLCons(2, LLCons(3, LLNil())))
    _cons[Int].modify(_.copy(_1 = 5))(l2) shouldBe LLNil()

    // if we want to know if `modify` has an effect, use `modifyOption`
    _cons[Int].modifyOption(_.copy(_1 = 5))(l1) shouldBe
      Some(LLCons(5, LLCons(2, LLCons(3, LLNil()))))
    _cons[Int].modifyOption(_.copy(_1 = 5))(l2) shouldBe None
  }

  "Prism with Lens" in {
    /* tuple is `Product` so, we can use Lens composed with Prism */

    /* first, seconds, ... */
    import monocle.function.Fields._ /* first, second, ... */
    import monocle.std.tuple2._      /* access to field instance for Tuple2 */

    (_cons[Int] composeLens first).set(5)(l1) shouldBe
      LLCons(5, LLCons(2, LLCons(3, LLNil())))

    (_cons[Int] composeLens first).set(5)(l2) shouldBe LLNil()
  }
}


object PrismTutorial {
  sealed trait Day
  case object Monday    extends Day
  case object Tuesday   extends Day
  case object Wednesday extends Day
  case object Thursday  extends Day
  case object Friday    extends Day
  case object Saturday  extends Day
  case object Sunday    extends Day
}
