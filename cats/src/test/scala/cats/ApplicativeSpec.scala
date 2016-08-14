package cats

import util.TestSuite

class ApplicativeSpec extends TestSuite {

  /**
    * Applicative extends the Apply type class with `pure`
    *
    * def pure[A](a: A): F[A]
    *
    *
    * Applicative is generallization of `Monad`,
    * allowing expression of effectful computation in a pure functional way
    *
    * Generally, preferred to `Monad` whe
    *
    */

  test("Applicative.pure") {
    import cats.implicits._

    (Applicative[List] compose Applicative[Option]).pure(1) shouldBe List(Some(1))
  }
}
