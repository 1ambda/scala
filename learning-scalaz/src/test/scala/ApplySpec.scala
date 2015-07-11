import org.scalatest.{FlatSpec, Matchers}

import scalaz.Apply
import scalaz.std.option._
import scalaz.std.list._
import scalaz.syntax.std.option._

// ref: https://github.com/scalaz/scalaz/blob/series/7.2.x/example/src/main/scala/scalaz/example/ApplyUsage.scala
class ApplySpec extends FlatSpec with Matchers {

  val intToString: Int => String = _.toString
  val double: Int => Int = _ * 2
  val addTwo: Int => Int = _ + 2

  // Apply defines the `ap` method which is similar to `map` from Functor.
  // But with `ap`, the applied function
  "Apply" should "apply functions in the context" in {

    // map
    Apply[Option].map(1.some)(intToString) shouldBe "1".some
    Apply[Option].map(1.some)(double) shouldBe 2.some
    Apply[Option].map(none)(double) shouldBe none

    // ap
    Apply[Option].ap(1.some)(some(intToString)) shouldBe "1".some
    Apply[Option].ap(1.some)(some(double)) shouldBe 2.some
    Apply[Option].ap(none)(some(double)) shouldBe none
    Apply[Option].ap(1.some)(none[Int => Int]) shouldBe none[Int]
    Apply[Option].ap(none)(none[Int => Int]) shouldBe none[Int]
    Apply[List].ap(List(1, 2 ,3))(List(double, addTwo)) shouldBe List(2, 4, 6, 3, 4, 5)

    import scalaz.syntax.apply._

    val plus1: Int => Int = _ + 1
    val plus2: Int => Int = _ + 2

    // <*> is syntax sugar for the `ap` method
    List(1, 2, 3) <*> List(plus1, plus2) shouldBe List(2, 3, 4, 3, 4, 5)
  }

  "Apply.apply" can "be used to lift a function" in {
    val add2 = ((_: Int) + (_: Int))
    val add3 = ((_: Int) + (_: Int) + (_: Int))
    val add4 = ((_: Int) + (_: Int) + (_: Int) + (_: Int))

    Apply[Option].apply2(some(1), some(2))(add2) shouldBe some(3)
    Apply[Option].apply3(some(1), some(2), some(3))(add3) shouldBe some(6)
    Apply[Option].apply4(some(1), some(2), some(3), some(4))(add4) shouldBe some(10)

    Apply[Option].apply3(some(1), none, some(3))(add3) shouldBe none

    import scalaz.syntax.apply._

    // ^, ^^, ^^^, ... is the syntax sugar for apply1, ...
    ^(1.some, 2.some)(_ + _) shouldBe 3.some
  }

  "applicative builder" should "allow yo evaluate a function of multiple arguments in a context" in {
    import scalaz.syntax.apply._

    (1.some |@| 2.some |@| 3.some)(_ + _ + _) shouldBe 6.some
    (1.some |@| none[Int] |@| 3.some)(_ + _ + _) shouldBe None
  }



}
