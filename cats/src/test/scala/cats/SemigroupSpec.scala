package cats

import util.TestSuite

class SemigroupSpec extends TestSuite {

  /**
    * Semigroup has a single operation
    * This operation must be guaranteed to be associative
    *
    * ((a combine b) combine c) is equal to
    * (a combine (b combine c))
    *
    * The result is, parallelism
    *
    */

  test("Semigroup.combine") {
    import cats.implicits._

    Semigroup[Int].combine(1, 2) should be (3)
    Semigroup[List[Int]].combine(List(1, 2, 3), List(4, 5, 6)) should be (List(1, 2, 3, 4, 5, 6))

    val x: Option[Int] = Some(3)
    val y: Option[Int] = Some(4)

    /**
      * The operation of Semigroup is various.
      * The effect of operation depends on the type inside of tye type class
      */
    val z = x match {
      case None => x
      case Some(xValue) => y match {
        case None => y
        case Some(yValue) => Some(xValue + yValue)
      }
    }

    /** SemigroupFunctions */
    Semigroup[Int => Int].combine(
      { (x: Int) => x + 1 },
      { (x: Int) => x * 10 }
    ).apply(6) should be (67)

    Map("foo" -> List(1, 2)) ++ Map("foo" -> List(4, 5)) should be (Map("foo" -> List(4, 5)))
    Map("foo" -> List(1, 2)).combine(Map("foo" -> List(4, 5))) should be (Map("foo" -> List(1, 2, 4, 5)))

    val one: Option[Int] = Option(1)
    val two: Option[Int] = Option(2)
    val n: Option[Int] = None

    one |+| two should be(Some(3))
    n |+| two should be(two)
    n |+| n should be(n)


  }


}
