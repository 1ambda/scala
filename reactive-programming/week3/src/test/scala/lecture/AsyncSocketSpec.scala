package lecture

import org.junit.runner.RunWith
import org.scalatest.concurrent.{ScalaFutures, AsyncAssertions}
import org.scalatest.time.{Seconds, Millis, Span}
import org.scalatest.{ShouldMatchers, FunSuite}
import org.scalatest.junit.JUnitRunner
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future
import scala.util.{Failure, Success}


@RunWith(classOf[JUnitRunner])
class AsyncSocketSpec extends FunSuite with ShouldMatchers with AsyncAssertions with ScalaFutures {
  import Http._
  val limit = timeout(Span(2000, Millis))

  // for whenReady
  implicit val defaultPatience = PatienceConfig(timeout = Span(2, Seconds))

  test("non-blocking1") {

    val socket = new AsyncSocket {}

    val response = for {
      packet <- socket.readFromMemory()
      result <- socket.send(socket.Europe, packet)
    } yield result

    whenReady(response) {
      res => res.toList should be (List(5, 6, 7, 8))
    }
  }

  test("non-blocking2") {
    val w = new Waiter

    val socket = new AsyncSocket {}
    val packet: Future[Array[Byte]] = socket.readFromMemory()

    packet.onComplete {
      case Success(p) =>
        val result: Future[Array[Byte]] = socket.send(socket.Europe, p)

        result.onComplete {
          case Success(b) =>
            b.toList should be (List(5, 6, 7, 8))
            w.dismiss()
          case Failure(t) =>
        }

      case Failure(t) =>
    }

    w.await(limit)
  }
}

