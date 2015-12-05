package free.withoutScalaz

import scalaz.{Functor, ~>, Monad}
import scalaz.syntax.all._

object Free1 {
  sealed trait Free[S[_], A] {
    def unit[A](a: A): Free[S, A] =
      Return(a)

    def flatMap[B](f: A => Free[S, B]): Free[S, B] = this match {
      case GoSub(a, g) => GoSub(a, (b: Any) => GoSub(g(b), f))
      case a => GoSub(a, f)
    }

    // a: Free[S, Z], g: Z => Free[S, A]
    // Free[S, B] == Free[S, Z], f: Z => Free[S, B]
    // Gosub(a, (b: Z) => Gosub(g(b), f)
    // g(b) = Free[S, A], f: A => Free[S, B]
    // GoSub(g(b), f) == Free[S, B]

    def map[B](f: A => B): Free[S, B] =
      flatMap(a => unit(f(a)))

    // S[Free[S, A]], fmap,  Free[S, Any] flatMap f
    //
    scalaz.Monad
    //
  }

  final case class Return[S[_], A](a: A) extends Free[S, A]
  final case class Suspend[S[_], A](a: S[Free[S, A]]) extends Free[S, A]
  final case class GoSub[S[_], A, B](a: Free[S, A], f: A => Free[S, B]) extends Free[S, B]
}

object Free2 {
  sealed trait Free[F[_], A] {
    def point[F[_]](a: A): Free[F, A] = Point(a)
    def flatMap[B](f: A => Free[F, B])(implicit functor: Functor[F]): Free[F, B] =
      this match {
        case Point(a)  => f(a)
        case Join(ffa) => Join(ffa.map(fa => fa.flatMap(f)))
      }
    def map[B](f: A => B)(implicit functor: Functor[F]): Free[F, B] =
      flatMap(a => Point(f(a)))
  }

  case class Point[F[_], A](a: A) extends Free[F, A]
  case class Join[F[_], A](ff: F[Free[F, A]]) extends Free[F, A]

  def liftF[F[_], A](a: => F[A])(implicit F: Functor[F]): Free[F, A] =
    Join(F.map(a)(Point[F, A]))

  def foldMap[F[_], M[_], A](fm: Free[F, A])(f: F ~> M)
                            (implicit FI: Functor[F], MI: Monad[M]): M[A] =
    fm match {
      case Point(a) => MI.pure(a)
      case Join(ffa) => MI.bind(f(ffa))(fa => foldMap(fa)(f))
    }
}

object Free3 {
  // https://github.com/scalaz/scalaz/blob/release/6.0.4/core/src/main/scala/scalaz/Free.scala
  final case class Return[S[+_], +A](a: A) extends Free[S, A]
  final case class Suspend[S[+_], +A](a: S[Free[S, A]]) extends Free[S, A]
  final case class Gosub[S[+_], A, +B](a: Free[S, A],
                                       f: A => Free[S, B]) extends Free[S, B]
  sealed trait Free[S[+_], +A] {
    final def map[B](f: A => B): Free[S, B] =
      flatMap(a => Return(f(a)))

    final def flatMap[B](f: A => Free[S, B]): Free[S, B] = this match {
      case Gosub(a, g) => Gosub(a, (x: Any) => Gosub(g(x), f))
      case a           => Gosub(a, f)
    }
  }
}
