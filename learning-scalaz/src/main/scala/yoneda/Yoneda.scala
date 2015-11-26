package yoneda

import scalaz.Functor

/**
 * ref - http://blog.higher-order.com/blog/2013/11/01/free-and-yoneda/
 */


trait Yoneda[F[_], A] {
  def map[B](f: A => B): F[B]
}

trait Coyoneda[F[_], A] {
  type I
  def f: I => A
  def fi: F[I]
}

/**
 * The Yoneda lemma says that
 * there is an isomorphism between Yoneda[F, A] and F[A]
 *
 * - for any functor `F`
 * - for any type `A`
 */

object Yoneda {
  def toYoneda[F[_]: Functor, A](fa: F[A]) = new Yoneda[F, A] {
    override def map[B](f: (A) => B): F[B] = Functor[F].map(fa)(f)
  }

  def fromYoneda[F[_], A](yo: Yoneda[F, A]): F[A] =
    yo.map(a => a)
}

/**
 * The Yoneda lemma says that
 * Coyoneda[F, A] is isomorphic to F[A]
 */

object Coyoneda {
  def toCoyoneda[F[_], A](fa: F[A]) = new Coyoneda[F, A] {
    override type I = A
    override def f: (I) => A = (a: A) => a
    override def fi: F[I] = fa
  }

  def fromCoyoneda[F[_]: Functor, A](coyo: Coyoneda[F, A]): F[A] =
    Functor[F].map(coyo.fi)(coyo.f)
}



