import org.scalatest.{FlatSpec, Matchers}

import scalaz.Apply
import scalaz.std.option._
import scalaz.std.list._
import scalaz.std.vector._
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

  "Writer" can "be used to write log" in {
    import scalaz.{WriterT, Writer, DList, Id}
    import scalaz.syntax.writer._
    import scalaz.syntax.apply._

    // ref: SO: http://stackoverflow.com/questions/3352418/what-is-a-dlist
    // Difference Lists are a list-like data structure that supports O(1) append operations
    type Logged[A] = Writer[DList[String], A]

    // log a message, return no results (hence Unit)
    def log(message: String): Logged[Unit] = DList(message).tell

    // log that we are adding, and return the results of adding x and y
    def compute(x: Int, y: Int): Logged[Int] =
      log("adding " + x + " and " + y) as (x+y)

    def addAndLog(x: Int, y: Int): Logged[Int] =
      log("begin") *> compute(x, y) <* log("end")

    val (written, sum) = addAndLog(1, 2).run
    written.toList shouldBe List("begin", "adding 1 and 2", "end")
    sum shouldBe 3
  }

  "Apply instance" can "be composed" in {
    val applyVLO = Apply[Vector] compose Apply[List] compose Apply[Option]

    val arg1 = Vector(List(1.some, none[Int]), List(2.some, 3.some))
    val arg2 = Vector(List("a".some, "b".some, "c".some))

    val deepResult = applyVLO.apply2(arg1, arg2)(_.toString + _)
    val expectedDeep = Vector(
      List(Some("1a"), Some("1b"), Some("1c"),
        None, None, None),
      List(Some("2a"), Some("2b"), Some("2c"),
        Some("3a"), Some("3b"), Some("3c")))

    deepResult shouldBe expectedDeep

  }

}
