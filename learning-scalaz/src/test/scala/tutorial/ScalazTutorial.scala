package tutorial

import org.scalatest._

// ref - http://www.slideshare.net/mpilquist/scalaz-13068563?related=1
class ScalazTutorial extends WordSpec with Matchers {

  "Option" in {
    // import functions & typeclass instances for option
    import scalaz.std.option._

    some(42) shouldBe Some(42)
    none[Int] shouldBe None

    val os = List(Some(42), None, Some(20))
    os.foldLeft(None: Option[Int]) { (acc, o) => acc orElse o } shouldBe some(42)
    os.foldLeft(none[Int]) { (acc, o) => acc orElse o } shouldBe some(42)
  }

  "Option conditional1" in {
    // import syntax extensions for option
    import scalaz.syntax.std.option._

    def xget(opt: Option[Int]) = opt | Int.MaxValue

    xget(Some(42)) shouldBe 42
    xget(None) shouldBe Int.MaxValue
  }

  "Option conditional2" in {
    // import functions, typeclass instances for primitive types
    import scalaz.std.anyVal._
    import scalaz.std.option._
    import scalaz.syntax.std.option._

    def zget(opt: Option[Int]) = ~opt /* getOrElse(zero) */

    zget(42.some) shouldBe 42
    zget(none) shouldBe 0

    // zero comes from Monoid
    import scalaz.Monoid
    import scalaz.std.string._
    import scalaz.std.list._

    def mzget[A : Monoid](opt: Option[A]) = ~opt
    mzget(none[Int])       shouldBe 0
    mzget(none[String])    shouldBe ""
    mzget(none[List[Int]]) shouldBe List()

    /*
      Monoid is a context bound for type A which means
      there must be an implicit value of type Monoid[A] in implicit scope

      def mzget[A](opt: Option[A])(implicit m: Monoid[A]) = ~opt

      even supports multiple bounds

      def other[A : Monoid : Functor] = ...
     */
  }

  "Option err" in {
    import scalaz.std.option._
    import scalaz.syntax.std.option._
    def knowBetter[A](opt: Option[A]) = opt err "You promised!"

    knowBetter(42.some)   shouldBe 42
    intercept[RuntimeException] {
      knowBetter(none[Int]) shouldBe "You promised!"
    }
  }

  "Option fold/cata" in {
    import java.net.InetAddress

    // replacement for the pattern matching
    def ipAddress(opt: Option[InetAddress]) =
      opt.fold("0.0.0.0")(_.getHostAddress)

    ipAddress(Some(InetAddress.getLocalHost)) should not be ("0.0.0.0")
    ipAddress(None)                           shouldBe "0.0.0.0"
  }

  "Option ifNone" in {
    import scalaz.std.option._
    import scalaz.syntax.std.option._

    var count = 0
    42.some ifNone { count += 1 }
    none ifNone { count += 1 }

    count shouldBe 1
  }

  "concatenate" in {
    import scalaz.Scalaz._
    // List(some(42), none, some(51)).sum /* doesn't compile */

    List(some(42), none, some(51)).concatenate
  }

  "sequence" in {
    import scalaz.Scalaz._

    List(42.some, none, 51.some).sequence shouldBe none
    List(42.some, 50.some, 51.some).sequence shouldBe List(42, 50, 51).some
  }

  /**
   * ref - http://yeghishe.github.io/2015/07/06/scalaz-semigroup-monoid-equal-order-enum-show-and-standard-scala-classes.html
   * ref - https://github.com/scalaz/scalaz/blob/series/7.1.x/core/src/main/scala/scalaz/Semigroup.scala
   *
   *  > Semigroup brings `append` operation and it should be associative
   *
   * trait SemigroupLaw {
   *    def associative(f1: F, f2: F, f3: F)(implicit F: Equal[F]): Boolean =
   *      F.equal(append(f1, append(f2, f3)), append(append(f1, f2), f3))
   * }
   *
   * def semigroupLaw = new SemigroupLaw {}
   */

  "Semigroup" in {
    import scalaz.Scalaz._

    3 |+| 4 shouldBe 7 /* append ops */
    List(1, 2) |+| List(3, 4) shouldBe List(1, 2, 3, 4)

    val lo = Map("odd" -> Set(1, 3), "even" -> Set(2, 4))
    val hi = Map("odd" -> Set(5, 7), "even" -> Set(6, 8))

    lo |+| hi shouldBe Map("odd" -> Set(1, 3, 5, 7), "even" -> Set(2, 4, 6, 8))
  }

  /**
   * Monoid extends Semigroup (so that Monoid has the associative append operation)
   *
   * See,
   * https://github.com/scalaz/scalaz/blob/series/7.2.x/core/src/main/scala/scalaz/std/Option.scala#75
   */
  "Option is Monoid" in {
    import scalaz._
    import Scalaz._

    some(20) |+| none |+| some(22) shouldBe some(42)
    // alternative Option monoid
    some(20).first |+| none[Int].first |+| some(22).first shouldBe some(20)
    some(20).last |+| none[Int].last |+| some(22).last    shouldBe some(22)
  }

  /**
   * Functor
   *  map: F[A] => (A => B) => F[B]
   *
   * Monad extends Functor (`flatMap(point)` will be `map`)
   *  point: A => M[A]
   *  flatMap: M[A] => (A => M[B]) => M[B]
   *
   * Applicative extends Functor (`ap(point(func))` will be `map`)
   *  point: A => F[A]
   *  ap: F[A] => F[A => B] => F[B]
   *
   *
   * Why Applicatives?
   *
   * - Less restrictive than Monads, and thus more general (powerful)
   * - Composable (http://tonymorris.github.io/blog/posts/monads-do-not-compose/)
   * - Support parallel computation
   */

  "Applicative Usage" in {
    import scalaz.std.option._
    import scalaz.syntax.applicative._

    val add5: Option[Int => Int] = Some((_: Int) + 5)
    some(15) <*> add5 shouldBe some(20) /* <*> is alias for `ap` */

    /* more convenient way: Applicative Builder */
    (some(15) |@| some(5)) apply { _ + _ } shouldBe some(20)
    (some(15) |@| none[Int] |@| some(6)) apply { _ + _ + _} shouldBe none[Int]

    case class Album(name: String, artist: String) {}

    (some("Jacky") |@| some("Bone")) apply Album.apply shouldBe Some(Album("Jacky", "Bone"))
  }

  /**
   * so why not just use Either?
   *
   * : When validation is constructed with an error type that has a Semigroup,
   *   there exists an Applicative Functor for Validation that accumulates errors
   */

  "Constructing Validations" in {
    import scalaz.Validation
    import scalaz.syntax.validation._

    42.success shouldBe Validation.success[Nothing, Int](42)
    "boom".failure shouldBe Validation.failure[String, Nothing]("boom")
  }

  "Option to Validation" in {
    import scalaz.std.option._
    import scalaz.syntax.std.option._
    import scalaz.Validation
    import scalaz.syntax.validation._

    42.some.toSuccess("boom") shouldBe Validation.success[String, Int](42)
    42.some.toSuccess("boom") shouldBe 42.success
    none[Int].toSuccess("boom") shouldBe "boom".failure

    42.some.toFailure("boom") shouldBe 42.failure
    none[Int].toFailure("boom") shouldBe "boom".success
  }

  "Either to Validation" in {
    import scalaz.Validation.fromEither
    import scalaz.syntax.validation._

    fromEither(Right(42)) shouldBe 42.success
    fromEither(Left("boom")) shouldBe "boom".failure
  }

  "Throwing to Validation" in {
    import scalaz.Validation.fromTryCatchThrowable
    import scalaz.syntax.validation._

    fromTryCatchThrowable[Int, Throwable]("42".toInt) shouldBe 42.success
    fromTryCatchThrowable[Int, Throwable]("asd".toInt).isFailure shouldBe true
  }

  "Mapping via Bifunction" in {
    import scalaz.Validation._
    import scalaz.syntax.validation._
    import scalaz.syntax.bifunctor._ /* <-: is alias for leftMap */

    fromTryCatchThrowable[Int, Throwable]("asd."toInt)
      .leftMap {_.getMessage } shouldBe "For input string: \"asd.\"".failure

    fromTryCatchThrowable[Int, Throwable]("asd."toInt).
      <-: { _.getMessage } shouldBe "For input string: \"asd.\"".failure

    fromTryCatchThrowable[Int, Throwable]("asd."toInt).
      bimap(_.getMessage, identity) shouldBe "For input string: \"asd.\"".failure
  }

  "Validation is not Monad, but it has flatMap (importing scalaz.Validation.FlatMap._)" in {
    import java.util.UUID
    import scalaz.Validation
    import Validation.fromTryCatchThrowable
    import Validation.FlatMap._
    import scalaz.syntax.std.option._
    import scalaz.syntax.validation._
    import scalaz.syntax.id._ /* import |> */

    def justMessage[S](v: Validation[Throwable, S]): Validation[String, S] =
      v.leftMap { _.getMessage }

    val meta1 = Map("id" -> "62ded0a0-67d6-11e5-b08c-0002a5d5c51b")
    val meta2 = Map("name" -> "lambda")
    val message = "No 'id' property"

    def extractUUID1(meta: Map[String, String]): Validation[String, UUID] =
      for {
        str <- meta.get("id").toSuccess("No 'id' property")
        id  <- justMessage(fromTryCatchThrowable[UUID, Throwable](UUID.fromString(str))) /* inside-out style */
      } yield id

    extractUUID1(meta1).isSuccess shouldBe true
    extractUUID1(meta2).isFailure shouldBe true
    extractUUID1(meta2) shouldBe message.failure

    def parseUUID(s: String): Validation[Throwable, UUID] =
      fromTryCatchThrowable[UUID, Throwable](UUID.fromString(s))

    def extractUUID(meta: Map[String, String]): Validation[String, UUID] =
      for {
        str <- meta.get("id").toSuccess("No 'id' property")
        id <- str |> parseUUID _ |> justMessage _
      } yield id

    extractUUID(meta1).isSuccess shouldBe true
    extractUUID(meta2).isFailure shouldBe true
    extractUUID(meta2) shouldBe message.failure
  }

  /**
   * Ref
   *  - http://stackoverflow.com/questions/12211776/why-isnt-validation-a-monad-scalaz7
   *  - https://groups.google.com/d/msg/scalaz/IWuHC0nlVws/syRUkXJklWIJ
   *  - https://gist.github.com/aappddeevv/7973370
   *
   * As of scalaz 7, `Validation` is not monad.
   * The roblem seems to be that `ap` would accumulate errors
   * whereas (pseudo-)monadic composition only operate on the value part of `Validation`
   *
   * The issue is that the applicative functor as implied by the monad
   * does not equal the actual applicative functor
   */

  // ref - https://github.com/bartschuller/scalaz-validation-example/blob/master/src/main/scala/PersonParser.scala
  "Validation as Applicative Functor" in {
    import scalaz._, Scalaz._

    val result: ValidationNel[String, String] = ("ok".successNel[String]
       |@| "fail1".failureNel[String]
       |@| "fail2".failureNel[String]) {_ + _ + _}
  }

  "Pipe" in {
    // http://stevegilham.blogspot.kr/2009/01/pipe-operator-in-scala.html
  }

  "scalaz" in {
    // TODO 107page
    // https://github.com/mpilquist/scalaz-talk/blob/master/examples.scala
    // https://higherkindedtripe.wordpress.com/2012/02/07/f-style-pipe-operator-in-scala/
  }

}
