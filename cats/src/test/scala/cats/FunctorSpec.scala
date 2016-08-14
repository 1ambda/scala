package cats

import util.TestSuite

class FunctorSpec extends TestSuite {

  /**
    * Functor is a simple type class that have `map` function
    *
    * map is a function that take F[A] and function1 which converts A to B
    *
    * the signature of map is,
    *
    * def map[A, B](fa: F[A])(f: A => B): F[B]
    *
    * like other type classes, the effect of map function is different.
    *
    */

  test("Functor.map") {
    import cats.implicits._

    /** implicit */ val optionFunctor: Functor[Option] = new Functor[Option] {
      def map[A, B](fa: Option[A])(f: A => B) = fa.map(f)
    }

    /** functions can also be created for types which don't have a map method */

    /** implicit */ def function1Functor[In]: Functor[Function1[In, ?]] =
      new Functor[Function1[In, ?]] {
        override def map[A, B](fa: (In) => A)(f: (A) => B): (In) => B = fa andThen f
      }
  }

  test("Functor.lift") {
    import cats.implicits._

    /**
      * def lift(f: A => B): F[A] => F[B] = map(_)(f)
      */
    val lenOption: Option[String] => Option[Int] = Functor[Option].lift(_.length)
    lenOption(Some("abcd")) should be (Some(4))
  }

  test("Functor.fproduct") {
    import cats.implicits._

    val m = List("cats", "is", "awesome").fproduct(_.length).toMap
    m.getOrElse("cats", 0) should be (4)
  }

  test("Functor.compose") {
    import cats.implicits._

    /**
      * Functors compose.
      *
      * Given any functor `F[_]` and any functor `G[_]`, we can create a new functor `F[G[_]]`
      *
      * Composing functor results in executing multiple effects at once without any additional effort
      */

    val listOpt = Functor[List] compose Functor[Option]

    listOpt.map(List(Some(1), None, Some(3)))(_ + 1) should be (
      List(Some(2), None, Some(4))
    )
  }
}
