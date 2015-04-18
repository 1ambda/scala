package lecture

import org.scalatest._

object MyOption {
  // ref: https://github.com/scala/scala/blob/2.11.x/src/library/scala/Option.scala#L333

  final case class MySome[+A](x: A) extends MyOption[A]
  final case object MyNone extends MyOption[Nothing] 

  abstract class MyOption[+T] {
    def flatMap[U](f: T => MyOption[U]): MyOption[U] = this match {
      case MySome(x) => f(x)
      case MyNone => MyNone
    }
  }
}

object MyTry {
  // ref: https://github.com/scala/scala/blob/2.11.x/src/library/scala/util/Try.scala
  import scala.util.control.NonFatal

  abstract class MyTry[+T] {
    def flatMap[U](f: T => MyTry[U]): MyTry[U] = this match {
      case MySuccess(x) => try f(x) catch { case NonFatal(ex) => MyFailure (ex) }
      case fail: MyFailure => fail
    }

    // t map f == t flatMap(f andThen MyTry)
    def map[U](f: T => U): MyTry[U] = this match {
      case MySuccess(x) => MyTry(f(x))
      case fail: MyFailure => fail
    }
  }


  case class MySuccess[T](x: T) extends MyTry[T]
  case class MyFailure(ex: Throwable) extends MyTry[Nothing]

  def apply[T](expr: => T): MyTry[T] =
    try MySuccess(expr)
    catch {
      case NonFatal(ex) => MyFailure(ex)
    }
}

class MonadTest extends FlatSpec with Matchers {


}
