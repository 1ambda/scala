package cats

import util.TestSuite

class ApplySpec extends TestSuite {

  /**
    * Apply extends Functor type class with `ap`
    *
    * def ap[A, B](ff: F[A => B])(fa: F[A]): F[B]
    *
    * we can define `product` using `ap`
    *
    * def product[A, B](fa: F[A], fb: F[B]): F[(A, B)] =
    *   ap(map(fa)(a => (b: B) => (a, b)))(fb)
    *
    * `product` is quite useful for combining values into the same context
    *
    * see `ap2` and `map2` implementation
    */

  test("Apply.ap") {
    import cats.implicits._

    val intToString: Int => String = _.toString
    val double: Int => Int = _ * 2
    val addTwo: Int => Int = _ + 2

    Apply[Option].ap(Some(intToString))(Some(1)) should be (Some("1"))
    Apply[Option].ap(None)(Some(1)) should be (None)
  }

  test("Apply: apN, mapN, tupleN") {
    import cats.implicits._

    val addArity2 = (a: Int, b: Int) ⇒ a + b
    val addArity3 = (a: Int, b: Int, c: Int) ⇒ a + b + c

    /**
      *
      * def map2[A, B, Z](fa: F[A], fb: F[B])(f: (A, B) => Z): F[Z] =
      *   map(product(fa, fb)) { case (a, b) => f(a, b) }
      *
      * def ap2[A, B, Z](ff: F[(A, B) => Z)(fa: F[A], fb: F[B]): F[Z] =
      *   map(product(fa, product(fb, ff))) { case (a, (b, f)) => f(a, b) }
      *
      */

    Apply[Option].ap2(Some(addArity2))(Some(1), Some(2)) shouldBe Some(3)
    Apply[Option].map2(Some(1), Some(2))(addArity2) shouldBe Some(3)
    Apply[Option].tuple2(Some(1), Some(2)) shouldBe Some((1, 2))
  }

  /**
    * the implication of Apply is,
    * It provides the way of manipulating multiple elements in the same context
    *
    * using `product` we can combine all elements into same the context
    * and using `map` we can manipulate them
    *
    * we can easily define mapN. For exmaple,
    *
    *   def map2[A, B, Z](fa: F[A], fb: F[B])(f: (A, B) => Z): F[Z] =
    *      map(product(fa, fb)) { case (a, b) => f(a, b) }
    */
  test("Apply Builder") {
    import cats.implicits._

    val merged = Option(1) |@| Option(2)

    merged map { _ + _ } shouldBe Some(3)
    merged apWith Some((a: Int, b: Int) => a + b) shouldBe Some(3)
    merged.tupled shouldBe Some((1, 2))
  }
}
