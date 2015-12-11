package monoid

import util.TestSuite

import scalaz._, Scalaz._


/**
 * ref
 * - http://www.slideshare.net/oxbow_lakes/practical-scalaz
 * - https://github.com/scalaz/scalaz/blob/series/7.3.x/example/src/main/scala/scalaz/example/TagUsage.scala
 */

class MonoidSpec extends TestSuite {
  test("Monoid[Map]") {

    val m1: Map[String, Int] = Map("1" -> 1, "2" -> 2)
    val m2: Map[String, Int] = Map("1" -> 0, "3" -> 3)

    m1 |+| m2 shouldBe Map("1" -> 1, "2" -> 2, "3" -> 3)
  }

  case class User(name: String, city: String)
  type Filter[A] = A => Boolean
  val users = List(User("Kelly", ".LONDON"), User("John", ".NY"), User("Cark", ".KAW"))

  test("Filters are monoid") {
    import Tags._
    import syntax.tag._

    val london: Filter[User] = (_: User).city endsWith(".LONDON")
    val ny: Filter[User]     = (_: User).city endsWith(".NY")

    implicit def monoidFilter[A] = new Monoid[Filter[A]] {
      override def zero: Filter[A] =
        a => (Monoid[Boolean @@ Disjunction].zero).unwrap // a => false
      override def append(f1: Filter[A], f2: => Filter[A]): Filter[A] =
        a => (Disjunction(f1(a)) |+| Disjunction(f2(a))).unwrap // a => f1(a) || f2(a)
    }

    (users filter (london |+| ny) size) shouldBe 2
  }

  test("Filter with Disjunction Monoid") {
    import Tags._
    import syntax.tag._

    implicit val filterMonoid = Monoid[String => Boolean @@ Disjunction]


    val london = (u: User) => Disjunction(u.city endsWith(".LONDON"))
    val ny     = (u: User) => Disjunction(u.city endsWith("NY"))

    (users filter { u => (london |+| ny)(u).unwrap }).size shouldBe 2
    (users filter { u => (london |+| ny)(u).unwrap }).size shouldBe 2
  }

  test("Filter can be implemented using for-comprehension") {

    val london = Reader((a: String) => a.endsWith(".LONDON"))
    val ny = Reader((a: String) => a.endsWith(".NY"))
    
    val filters = List(london, ny)

    val result = for {
      u <- users
      f <- filters /* inefficient */
      if f(u.city)
    } yield u

    result.size shouldBe 2
  }

  test("Monoid Tag") {
    import Tags._

    Multiplication(3) |+| Multiplication(3) shouldBe Multiplication(9)
    Monoid[Int @@ Multiplication].zero shouldBe Multiplication(1)


    /** Conjuction, && */
    Conjunction(true) |+| Conjunction(true) shouldBe Conjunction(true)
    Conjunction(false) |+| Conjunction(true) shouldBe Conjunction(false)
    Conjunction(true) |+| Conjunction(false) shouldBe Conjunction(false)
    Conjunction(false) |+| Conjunction(false) shouldBe Conjunction(false)
    Monoid[Boolean @@ Conjunction].zero shouldBe Conjunction(true)

    /** Disjunction, || */
    Disjunction(true) |+| Disjunction(true) shouldBe Disjunction(true)
    Disjunction(false) |+| Disjunction(true) shouldBe Disjunction(true)
    Disjunction(true) |+| Disjunction(false) shouldBe Disjunction(true)
    Disjunction(false) |+| Disjunction(false) shouldBe Disjunction(false)
    Monoid[Boolean @@ Disjunction].zero shouldBe Disjunction(false)

    // subst[F[_], A](fa: F[A]): F[A @@ T]
    Conjunction.subst(List(true, true, true)).suml shouldBe Conjunction(true)
    Conjunction.subst(List(true, false, true)).suml shouldBe Conjunction(false)

    Disjunction.subst(List.empty[Boolean]).suml shouldBe Disjunction(false)

    Disjunction.unwrap(Disjunction(true)) shouldBe true

    val min = MinVal(3) |+| MinVal(1) |+| MinVal(5)
    min shouldBe MinVal(1)
    MinVal.unwrap(min) shouldBe 1

    sealed trait Sorted
    val Sorted: Tag.TagOf[Sorted] = Tag.of[Sorted]
    def sortList[A: scala.math.Ordering](as: List[A]): List[A] @@ Sorted =
      Sorted(as.sorted)

    def minOption[A](a: List[A] @@ Sorted): Option[A] =
      Sorted.unwrap(a).headOption

    // we can also use pattern matching to extract value from a tag
    def minOption_[A]:  List[A] @@ Sorted => Option[A] = {
      case Sorted(xs) => xs.headOption
    }

    minOption(sortList((List(3, 2, 1, 5, 3)))) shouldBe Some(1)
    minOption_(sortList((List(3, 2, 1, 5, 3)))) shouldBe Some(1)
  }

  test("getOrElse M.zero") {
    val o1 = 1.some
    val o2 = none[Int]

    ~o1 shouldBe 1
    ~o2 shouldBe 0
  }

  test("OptinoOps, BooleanOps: ? and | and !") {
    (1.some | 0) shouldBe 1 /* getOrElse */
    (true  ? 1 | 2) shouldBe 1
    (false ? 1 | 2) shouldBe 2
    (true  ?? 1) shouldBe 1
    (false ?? 1) shouldBe 0 /* raise into zero */
    (true  !? 1) shouldBe 0 /* reversed `??` */
    (false !? 1) shouldBe 1
  }

  /**

    final case class Endo[A](run: A => A) {
      final def apply(a: A): A = run(a)
      final def compose(other: Endo[A]): Endo[A] = Endo.endo(run compose other.run)
      final def andThen(other: Endo[A]): Endo[A] = other compose this
    }
   */
  test("BooleanW + Endo") {

  }
}
