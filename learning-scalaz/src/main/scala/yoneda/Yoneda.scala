package yoneda

import scalaz.Functor

object Yoneda {
  // https://github.com/scalaz/scalaz/blob/series/7.1.x/core/src/main/scala/scalaz/Yoneda.scala

abstract class Yoneda[F[_], A] { yo =>
  def apply[B](f: A => B): F[B]

  def run: F[A] = apply(a => a)

  def map[B](f: A => B): Yoneda[F, B] = new Yoneda[F, B] {
    override def apply[C](g: (B) => C): F[C] = yo(f andThen g)
  }
}

/** `Yoneda[F, _]` is a functor for any `F` */
implicit def yonedaFunctor[F[_]]: Functor[({ type  λ[α] = Yoneda[F,α]})#λ] =
  new Functor[({type λ[α] = Yoneda[F, α]})#λ] {
    override def map[A, B](ya: Yoneda[F, A])(f: A => B): Yoneda[F, B] =
      ya map f
  }

/** `F[A]` converts to `Yoneda[F, A]` for any functor `F` */
def apply[F[_]: Functor, A](fa: F[A]): Yoneda[F, A] = new Yoneda[F, A] {
  override def apply[B](f: A => B): F[B] = Functor[F].map(fa)(f)
}

/** `Yoneda[F, A]` converts to `F[A` for any `F` */
def from[F[_], A](yo: Yoneda[F, A]): F[A] =
  yo.run
}

object Coyoneda {
sealed abstract class Coyoneda[F[_], A] { coyo =>
  type I
  val fi: F[I]
  val k: I => A

  final def map[B](f: A => B): Aux[F, I, B] =
    apply(fi)(f compose k)

  final def run(implicit F: Functor[F]): F[A] =
    F.map(fi)(k)
}

type Aux[F[_], A, B] = Coyoneda[F, B] { type I = A }

def apply[F[_], A, B](fa: F[A])(_k: A => B): Aux[F, A, B] =
  new Coyoneda[F, B] {
    type I = A
    val k = _k
    val fi = fa
  }

/** `F[A]` converts to `Coyoneda[F, A]` for any `F` */
def lift[F[_], A](fa: F[A]): Coyoneda[F, A] = apply(fa)(identity[A])

/** `Coyoneda[F, A]` converts to `F[A]` for any Functor `F` */
def from[F[_], A](coyo: Coyoneda[F, A])(implicit F: Functor[F]): F[A] =
  F.map(coyo.fi)(coyo.k)

/** `CoyoYoneda[F, _]` is a functor for any `F` */
implicit def coyonedaFunctor[F[_]]: Functor[({ type  λ[α] = Coyoneda[F,α]})#λ] =
  new Functor[({type λ[α] = Coyoneda[F, α]})#λ] {
    override def map[A, B](ca: Coyoneda[F, A])(f: A => B): Coyoneda[F, B] =
      ca.map(f)
  }
}

object Yoneda1 {
  /**
  * TODO
    *
  * https :// github.com / scalaz / scalaz / blob / c847654dcf75c748eacbaf246511bbd938b8631f / core / src / main / scala / scalaz / Yoneda.scala
  * https :// github.com / scalaz / scalaz / blob / c847654dcf75c748eacbaf246511bbd938b8631f / core / src / main / scala / scalaz / Coyoneda.scala
  *
  * http :// www.slideshare.net / kenbot / category - theory - for -beginners
  * http :// www.slideshare.net / kenbot / your - data - structures - are - made - of - maths ? related = 1
  */

  /**
   * ref
   *
   * - http://blog.higher-order.com/blog/2013/11/01/free-and-yoneda/
   * - http://stackoverflow.com/questions/24000465/step-by-step-deep-explain-the-power-of-coyoneda-preferably-in-scala-throu
   */


  trait Yoneda[F[_], A] {
    def run[B](f: A => B): F[B]
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
    def toYoneda[F[_] : Functor, A](fa: F[A]) = new Yoneda[F, A] {
      override def run[B](f: (A) => B): F[B] = Functor[F].map(fa)(f)
    }

    def fromYoneda[F[_], A](yo: Yoneda[F, A]): F[A] =
      yo.run(a => a)
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

    def fromCoyoneda[F[_] : Functor, A](coyo: Coyoneda[F, A]): F[A] =
      Functor[F].map(coyo.fi)(coyo.f)
  }
}
