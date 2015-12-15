package monoid

import util.FunTestSuite

import scalaz._, Scalaz._


/**
 * ref
 * - http://www.slideshare.net/oxbow_lakes/practical-scalaz
 * - https://github.com/scalaz/scalaz/blob/series/7.3.x/example/src/main/scala/scalaz/example/TagUsage.scala
 * - https://github.com/json4s/json4s/tree/3.4/scalaz
 */

class MonoidSpec extends FunTestSuite {

  import scalaz.syntax.equal._

  test("Monoid[Map]") {

    val m1: Map[String, Int] = Map("1" -> 1, "2" -> 2)
    val m2: Map[String, Int] = Map("1" -> 0, "3" -> 3)

    m1 |+| m2 shouldBe Map("1" -> 1, "2" -> 2, "3" -> 3)
  }

  type Filter[A] = A => Boolean
  case class User(name: String, city: String)
  val users = List(
    User("Kelly", ".LONDON"),
    User("John", ".NY"),
    User("Cark", ".SEOUL"),
    User("Kelly", ".NY"),
    User("Kelly", ".SEOUL")
  )

  test("Filters are monoid") {
    import Tags._ ,syntax.tag._
    import std.anyVal._, std.function._

    val london: Filter[User] = _.city endsWith(".LONDON")
    val ny: Filter[User]     = _.city endsWith(".NY")
    val isKelly = (_: User).name endsWith("Kelly")

    implicit def booleanMonoid[A] = function1Monoid[A, Boolean](booleanInstance.disjunction)

    implicit class FilterOps[A](fa: Function1[A, Boolean]) {
      def |*|(other: Function1[A, Boolean]): Function1[A, Boolean] =
        function1Monoid[A, Boolean](booleanInstance.conjunction).append(fa, other)
    }

    ((users filter (london |+| isKelly) size)) shouldBe 3
    ((users filter (london |+| ny) size)) shouldBe 3

    val ks1 = users filter ((london |*| isKelly) |+| (ny |*| isKelly))
    val ks2 = users filter ((london |+| ny) |*| isKelly)

    ks1 === ks2
    ks1 shouldBe ks2
    ks1.size shouldBe 2
  }

  test("Filter with Disjunction Monoid") {
    import Tags._
    import syntax.tag._

    val london = (u: User) => Disjunction(u.city endsWith(".LONDON"))
    val ny     = (u: User) => Disjunction(u.city endsWith("NY"))

    (users filter { u => (london |+| ny)(u).unwrap }).size shouldBe 3
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

    result.size shouldBe 3
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

  test("manipulate JSON with BooleanW") {
    import org.json4s.scalaz.JsonScalaz._
    import org.json4s._
    import org.json4s.jackson.JsonMethods._

    val json = parse(
      """
        {
          "street": "Manhattan 2",
          "zip": "00223"
        }
      """)

    case class Address(street: String, zipCode: String)
    case class Person(name: String, age: Int, address: Address)

    val a1 = (field[String]("street")(json) |@| field[String]("zip")(json)) apply Address
    val a2 = (field[String]("streets")(json) |@| field[String]("zip")(json)) apply Address

    a1.isSuccess shouldBe true
    a2.isFailure shouldBe true

    val expectedAddress = Success(Address("Manhattan 2", "00223"))
    Address.applyJSON(field[String]("street"), field[String]("zip"))(json) shouldBe expectedAddress

    implicit def addrJSONR: JSONR[Address] = Address.applyJSON(field[String]("street"), field[String]("zip"))

    val p = parse(
      """
        { "name":"joe",
          "age":34,
          "address": {
            "street": "Manhattan 2", "zip": "00223"
          }
        }
      """)
    val expectedPerson = Success(Person("joe", 34, Address("Manhattan 2", "00223")))
    Person.applyJSON(field[String]("name"), field[Int]("age"), field[Address]("address"))(p) shouldBe expectedPerson
  }

  /**

  final case class Endo[A](run: A => A) {
      final def apply(a: A): A = run(a)
      final def compose(other: Endo[A]): Endo[A] = Endo.endo(run compose other.run)
      final def andThen(other: Endo[A]): Endo[A] = other compose this
    }

    final def ??[A](a: => A)(implicit z: Monoid[A]): A =
      b.valueOrZero(self)(a)
    final def valueOrZero[A](cond: Boolean)(value: => A)(implicit z: Monoid[A]): A =
      if (cond) value else z.zero

    */
  test("BooleanW + Endo") {
    val neg = Endo(-(_: Double))

    (true ?? neg apply 2.3) shouldBe -2.3
    (false ?? neg apply 2.3) shouldBe 2.3


    case class HttpRequest(gzipped: Boolean) {
      def unzip: HttpRequest = this.copy(gzipped = false)
    }
    case class HttpResponse()
    type Handler = HttpRequest => HttpResponse

    def handleRequest_(req: HttpRequest, handler: Handler): HttpResponse =
      handler(req.gzipped ? req.unzip | req)

    def handlerRequest(req: HttpRequest, handler: Handler): HttpResponse =
      handler(req.gzipped ?? Endo[HttpRequest](_.unzip) apply req)
  }

  test("tag") {
    Tag
  }
}
