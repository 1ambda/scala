package free

trait TailRecursive[A] {
  def unit(a: A): TailRecursive[A] =
    Return(a)

  def flatMap[B](f: A => TailRecursive[B]): TailRecursive[B] =
    GoSub(this, f)

  def map[B](f: A => B): TailRecursive[B] =
    flatMap(f andThen (Return(_))) /* flatMap(a => Return(f(a))) */
}

final case class Return[A](a: A) extends TailRecursive[A]
final case class Suspend[A](s: () => A) extends TailRecursive[A]
final case class GoSub[A, B](ta: TailRecursive[A],
                             f: A => TailRecursive[B]) extends TailRecursive[B]

object TailRecursive {
  @annotation.tailrec
  def run[A](t: TailRecursive[A]): A = t match {
    case Return(a) => a
    case Suspend(s) => s()
    case GoSub(x, f) => x match {
      case Return(a) => run(f(a))
      case Suspend(s) => run(f(s()))
      case GoSub(y, g) => run(y flatMap(a => g(a) flatMap f)) /* GoSub(GoSub(y, g), f) */
    }
  }
}


