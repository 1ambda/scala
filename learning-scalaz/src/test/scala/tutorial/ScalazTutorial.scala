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

}
