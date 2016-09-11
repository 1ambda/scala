package cats

import util.TestSuite

import scala.concurrent.Await
import scala.util.Success

class MonadSpec extends TestSuite {

  /**
    * Monad extends Applicative (`pure`, `map`) with `flatten`
    *
    * def flatten[A](ffa: F[F[A]]): F[A]
    *
    * we can define `flatMap` using `flatten` with `map`
    *
    * def flatMap(fa: F[A])(f: A => F[B]): F[B] =
    *   flatten(map(fa)(f))
    *
    * in other word, Monad knows how to extract value from context while executing effect
    * since monad is so powerful as you can see above, it also has some limitations
    *
    * - Unlike `Functor`s and `Applicative`s, not all `Monad`s compose.
    * This means that even if `M[_]` and `N[_]` are both `Monad`s, `M[N[_]]` is not guaranteed to be a `Monad`
    *
    */

  test("Monad.flatten") {
    Option(Option(1)).flatten shouldBe Some(1)
    Option(None).flatten shouldBe None

    Some(1).flatMap
    List(List(1), List(2, 3)).flatten shouldBe List(1, 2, 3)
  }

  test("Monad.ifM") {
    import cats.std.all._

    Monad[Option].ifM(Option(true))(
      Option("true"), Option("false")
    ) shouldBe Option("true")

    Monad[List].ifM(List(true, false, true))(
      List(1, 2), List(3, 4)
    ) shouldBe List(1, 2, 3, 4, 1, 2)
  }

  test("Monads (in general) do not compose") {
    trait Functor[F[_]] {
      def map[A, B](fa: F[A])(f: A => B): F[B]
    }

    trait Apply[F[_]] extends Functor[F] {
      def ap[A, B](fa: F[A])(ff: F[A => B]): F[B]

      def product[A, B](fa: F[A], fb: F[B]): F[(A, B)] =
        ap(fb)(map(fa)(a => (b: B) => (a, b)))

      def map2[A, B, Z](fa: F[A], fb: F[B])(f: (A, B) => Z): F[Z] =
        map(product(fa, fb)) { case (a, b) => f(a, b) }
    }

    trait Applicative[F[_]] extends Apply[F] {
      def pure[A](a: A): F[A]
    }

    trait Monad[F[_]] extends Applicative[F] {
      def flatten[A](ffa: F[F[A]]): F[A]

      def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B] =
        flatten(map(fa)(f))
    }

    /**
      * ref - http://blog.tmorris.net/posts/monads-do-not-compose/
      *
      * def XCompose[M[_], N[_]](implicit mx: X[M], nx: X[N]):
      *   X[ λ[A => M[N[A]]] ]
      */

    trait FunctorCompose[M[_], N[_]]
      extends Functor[ λ[A => M[N[A]]] ] {

      def M: Functor[M]
      def N: Functor[N]

      override def map[A, B](fa: M[N[A]])(f: (A) => B): M[N[B]] =
        M.map(fa)(na => N.map(na)(f))
    }

    trait ApplyCompose[M[_], N[_]]
      extends Apply [ λ[A => M[N[A]]] ] with FunctorCompose[M, N] {

      def M: Apply[M]
      def N: Apply[N]

      /** that means, we can convert M[N[A] => N[B]] to M[N[A => B]] */
      def ap[A, B](a: M[N[A]])(ff: M[N[A => B]]): M[N[B]] =
        M.ap(a)(M.map(ff)((fn: N[A => B]) => (na: N[A]) => N.ap(na)(fn)))
    }

    trait ApplicativeCompose[M[_], N[_]]
      extends Applicative [ λ[A => M[N[A]]] ] with ApplyCompose[M, N] {

      def M: Applicative[M]
      def N: Applicative[N]

      def pure[A](a: A): M[N[A]] = M.pure(N.pure(a))
    }
  }

  test("Monad Transformer: OptionT") {
    /**
      * However, it is common to want to compose the effects of both `M[_]` and `N[_]`
      *
      * There are some alternatives, but simply we can use monad transformer.
      *
      * Monad transformer used to compose effects encapsulated by monads
      * such as state, exception handling, and I/O in a modular way
      *
      * `OptionT[F[_], A]` is a wrapper on an `F[Option[A]]` in the example below
      *
      * Monad transformer takes a monad as a type argument and return the result monad
      */

    case class OptionT[F[_], A](value: F[Option[A]])

    implicit def optionTMonad[F[_]](implicit F: Monad[F]) = {
      new Monad[OptionT[F, ?]] {
        override def pure[A](a: A): OptionT[F, A] =
          OptionT(F.pure(Some(a)))

        override def flatMap[A, B](fa: OptionT[F, A])(f: A => OptionT[F, B]): OptionT[F, B] =
          OptionT {
            F.flatMap(fa.value) {
              case None => F.pure(None)
              case Some(a) => f(a).value
            }
          }
      }

      val listOpt: OptionT[List, Int] = OptionT(List(Some(1), None, Some(3)))
    }
  }

  /** ref - https://github.com/typelevel/cats/blob/master/docs/src/main/tut/optiont.md */
  test("Option Usage") {
    import scala.concurrent.Future
    import scala.concurrent.ExecutionContext.Implicits.global

    import cats.data.OptionT
    import cats.implicits._

    val customGreeting: Future[Option[String]] = Future.successful(Some("welcome back, Lola"))
    val customGreetingT: OptionT[Future, String] = OptionT(customGreeting)

    val boringGreeting: Future[Option[String]] = customGreeting.map(opt => opt.map(_ + "1"))
    val excitedGreeting: OptionT[Future, String] = customGreetingT.map(_ + "1")

    /** lifting example */
    val greetingFO: Future[Option[String]] = Future.successful(Some("Hello"))
    val firstnameF: Future[String] = Future.successful("Jane")
    val lastnameO: Option[String] = Some("Doe")

    val ot: OptionT[Future, String] = for {
      g <- OptionT(greetingFO)
      f <- OptionT.liftF(firstnameF)
      l <- OptionT.fromOption[Future](lastnameO)
    } yield s"$g $f $l"
  }
}
