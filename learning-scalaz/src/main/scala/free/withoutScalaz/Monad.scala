package free.withoutScalaz

trait Monad1[F[_]] {
  def point[A](a: A): F[A]
  def bind[A, B](fa: F[A])(f: A => F[B]): F[B]

  def join[A](ffa: F[F[A]]): F[A] =
    bind(ffa)(a => a)
  def map[A, B](fa: F[A])(f: A => B): F[B] =
    bind(fa)(a => point(f(a)))
}

trait Monad2[F[_]] {
  def point[A](a: A): F[A]
  def join[A](ffa: F[F[A]]): F[A]
  def map[A, B](fa: F[A])(f: A => B): F[B]

  def bind[A, B](fa: F[A])(f: A => F[B]): F[B] =
    join(map(fa)(f))
}

