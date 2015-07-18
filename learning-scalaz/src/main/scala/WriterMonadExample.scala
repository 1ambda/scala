// ref: http://tonymorris.github.io/blog/posts/the-writer-monad-using-scala-example/

trait Monoid[A] {
  def append(a1: A, a2: A): A
  def empty: A
}

object Monoid {
  implicit def ListMonoid[A]: Monoid[List[A]] = new Monoid[List[A]] {
    def append(a1: List[A], a2: List[A]) = a1 ::: a2
    def empty = Nil
  }
}

case class Logger[L, A](log: L, value: A) {
  def map[B](f: A => B) = Logger(log, f(value))

  def flatMap[B](f: A => Logger[L, B])(implicit m: Monoid[L]) = {
    val b = f(value)
    Logger(m.append(log, b.log), b.value)
  }
}

object Logger {
  def unit[L, A](value: A)(implicit m: Monoid[L]) = Logger(m.empty, value)
}

object LoggerUtil {
  implicit def ListLogUtil[A](a: A) = new {
    def ~>[B](b: B) = Logger(List(a), b)

    def <~[B](f: A => B) = Logger(List(f(a)), a)
  }

  def noLog[A](a: A) = Logger.unit[List[String], A](a)
}

object WriterMonadExample extends App {
  import LoggerUtil._

  val x = 3;

  val r = for {
    a <- addOne(x)
    b <- intString(a)
    c <- lengthIsEven(b)
    d <- noLog(hundredOrThousand(c))
    e <- times(7)(d)
  } yield e

  println(s"RESULT: ${r.value}")
  println
  println(s"LOG")
  println("---")
  r.log foreach println

  def addOne(n: Int) =
    ("adding one to " + n) ~> (n + 1)

  def intString(n: Int) =
    ("converting int to string " + n) ~> n.toString

  def lengthIsEven(s: String) =
    ("checking length of " + s + " for evenness") ~> (s.length % 2 == 0)

  def hundredOrThousand(b: Boolean) = // no logging
    if(b) 100 else 1000

  def times(times: Int)(n: Int) =
    (n * 7) <~ ("multiplying " + n + " by 7 to produce " + _)
}
