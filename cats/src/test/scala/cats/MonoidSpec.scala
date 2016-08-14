package cats

import util.TestSuite

class MonoidSpec extends TestSuite {

  /**
    * Monoid extends Semigroup type class while adding zero (or sometimes called empty) operation.
    * Zero provide the identity functionality to combine operation
    *
    * (combine(x, empty) == combine(empty, x) == x)
    *
    * For example, Monoid[String] with `combine` defined as string concat
    * empty is == ""
    *
    * Havning an `empty` defined allow us to combine all elements
    * and fallback to `empty` if collection is empty instead of returning Option[T]
    *
    */

  test("Monoid.empty") {
    import cats.implicits._

    Monoid[String].empty should be("")
    Monoid[String].combineAll(List("a", "b", "c")) should be ("abc")

    /** xs.foldLeft(zero)(combine) */
    Monoid[Map[String, Int]].combineAll(
      List(
        Map("a" -> 1, "b" -> 2),
        Map("a" -> 3)
      )
    ) should be (Map("a" -> 4, "b" -> 2))

    val l = List(1, 2, 3, 4, 5)
    l.foldMap(identity) should be (15)
    l.foldMap(i => i.toString) should be ("12345")

    /** implicit */ def monoidTuple[A: Monoid, B: Monoid]: Monoid[(A, B)] =
      new Monoid[(A, B)] {
        override def empty: (A, B) = (Monoid[A].empty, Monoid[B].empty)
        override def combine(x: (A, B), y: (A, B)): (A, B) = {
          val (xa, xb) = x
          val (ya, yb) = y

          (Monoid[A].combine(xa, ya), Monoid[B].combine(xb, yb))
        }
      }

    l.foldMap(i => (i, i.toString)) should be ((15, "12345"))
  }


}
