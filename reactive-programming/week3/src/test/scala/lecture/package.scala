import scala.concurrent.Future
import scala.util.Try
import scala.concurrent.ExecutionContext.Implicits.global

package object lecture {
  // implicit value classes
  // ref: http://www.blog.project13.pl/index.php/coding/1769/scala-2-10-and-why-you-will-love-implicit-value-classes/
  implicit class TryCompanionOps[T](val t: Try.type) extends AnyVal {
    def convert[T](f: => Future[T]): Future[Try[T]] = f.map(value => Try(value))
  }
}

