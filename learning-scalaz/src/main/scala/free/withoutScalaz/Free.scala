package free.withoutScalaz

sealed trait Free[S[_], A] {
  def unit[A](a: A): Free[S, A] =
    Return(a)

  def flatMap[B](f: A => Free[S, B]): Free[S, B] = this match {
    case GoSub(a, g) => GoSub(a, (b: Any) => GoSub(g(b), f))
    case a => GoSub(a, f)
  }

  // GoSub(a, k)
  // GoSub(a, k =>
  //        GoSub(a, (c: Any) => GoSub(k(c), h))
  def map[B](f: A => B): Free[S, B] =
    flatMap(a => unit(f(a)))
}

final case class Return[S[_], A](a: A) extends Free[S, A]
final case class Suspend[S[_], A](a: S[Free[S, A]]) extends Free[S, A]
final case class GoSub[S[_], A, B](a: Free[S, A], f: A => Free[S, B]) extends Free[S, B]





