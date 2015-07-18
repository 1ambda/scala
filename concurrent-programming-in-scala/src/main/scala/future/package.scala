import scala.concurrent.{Promise, Future}
import scala.util.Success

package object future {
  import scala.concurrent.ExecutionContext.Implicits.global

  implicit class FutureOps[T](val self: Future[T]) {
    def or(that: Future[T]): Future[T] = {
      val p = Promise[T]

      self onComplete { case x => p tryComplete x }
      that onComplete { case y => p tryComplete y }

      p.future
    }
  }
}
