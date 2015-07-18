import org.scalatest.{Matchers, FunSuite}


// ref: http://www.slideshare.net/StackMob/monad-transformers-in-the-wild
class MonadTransformerSpec extends FunSuite with Matchers {
  /* Monad Signature

     trait Monad[F[_]] extends Applicative[F] {
        def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]
     }
   */

  test("Monad examples") {
    // import cheat sheet: http://eed3si9n.com/scalaz-cheat-sheet
    import scalaz.Monad
    import scalaz.std.option._
    import scalaz.syntax.std.option._

    Monad[Option].point(1) shouldBe 1.some
  }
}
