// http://docs.scala-lang.org/overviews/core/futures.html

package future

import org.scalatest._
import org.scalatest.concurrent._

import scala.concurrent.{ Future, Await }
import scala.concurrent.duration._
import scala.util. { Success, Failure }
import scala.concurrent.ExecutionContext.Implicits.global
import Book._ 

class FutureTest extends FreeSpec with Matchers with ScalaFutures {

  val books = List(
    Book("Akka in Action", 45.90),
    Book("Meta Object Protocol", 35.00),
    Book("Learning scalaz", 25.49)
  )

  val expected = 106.39

  "blocking" in {

    val f = Future {
      Book.getBookListPrice(books)
    }

    // blocking
    val price = Await.result(f, 1.second)

    price should be (expected)
  }

  "non-blocking" in {
    val f = Future {
      Book.getBookListPrice(books)
    }

    f.onComplete {
      case Success(price) => price should be (expected)
      case Failure(t: Throwable) => t.getMessage should be ("error01")
    }
  }
}
