package free.withoutScalaz

trait Monad[M[_]] {
  def unit[A](a: A): M[A]
  def flatMap[A, B](a: M[A])(f: A => M[B]): M[B]
  def map[A, B](a: M[A])(f: A => B): M[B]
}

object Monad {
  def apply[M[_]: Monad]: Monad[M] = implicitly[Monad[M]]
}

