package lecture

import org.junit.runner.RunWith
import org.scalatest.concurrent.AsyncAssertions
import org.scalatest.time.{Span, Seconds}
import org.scalatest.{ParallelTestExecution, FunSuite, Matchers}
import org.scalatest.junit.JUnitRunner

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Try, Failure, Success}

@RunWith(classOf[JUnitRunner])
class ResilientSocketSpec extends FunSuite
with Matchers with AsyncAssertions with mailServer with ParallelTestExecution {

  val packet: Array[Byte]     = List(1, 2, 3, 4).map(_.toByte).toArray
  val expected: Array[Byte] = List(5, 6, 7, 8).map(_.toByte).toArray

  val limit = timeout(Span(2, Seconds))
  val socket = new ResilientSocket {}

  test("retry using await and async") {
    val w = new Waiter

    val confirm = for {
      packet <- socket.asyncReadFromMemory()
      confirm <- socket.awaitRetry(10) { socket.send(Europe, packet)}
    } yield confirm

    confirm.onComplete {
      case Success(response) =>
        response should be (expected)
        w.dismiss()
      case Failure(t: FailToSendException) =>
        t.url should be (Europe)
        w.dismiss()
    }

    w.await(limit)
  }

  test("retry using foldRight and fallbackTo") {
    val w = new Waiter

    val confirm = for {
      packet <- socket.asyncReadFromMemory()
      confirm <- socket.foldRightRetry(10) { socket.send(Europe, packet) }
    } yield confirm

    confirm.onComplete {
      case Success(response) =>
        response should be (expected)
        w.dismiss()
      case Failure(t: FailToSendException) =>
        t.url should be (Europe)
        w.dismiss()
    }

    w.await(limit)
  }

  test("retry using recursion") {
    val w = new Waiter

    val confirm = for {
      packet <- socket.asyncReadFromMemory()
      confirm <- socket.recursiveRetry(10) { socket.send(Europe, packet) }
    } yield confirm

    confirm.onComplete {
      case Success(response) =>
        response should be (expected)
        w.dismiss()
      case Failure(t: FailToSendException) =>
        t.url should be (Europe)
        w.dismiss()
    }

    w.await(limit)
  }

  test("Awaitable") {
    val confirm = for {
      packet <- socket.asyncReadFromMemory()
      confirm <- socket.sendToFallback(packet)
    } yield confirm

    val response: Array[Byte] = Await.result(confirm, 2 seconds)
    response should be (expected)
  }

  test("sendToFallback") {
    val w = new Waiter

    val confirm = for {
      packet <- socket.asyncReadFromMemory()
      confirm <- socket.sendToFallback(packet)
    } yield confirm

    confirm.onComplete {
      case Success(p) =>
        p should be (expected)
        w.dismiss()

      case Failure(t) =>
        fail()
        w.dismiss()
    }

    w.await(limit)
  }

  test("sendToRecover") {
    val w = new Waiter

    val confirm: Future[Array[Byte]] = socket.sendToRecover(packet)

    confirm.onComplete {
      case Success(p) =>
        p should be (expected)
        w.dismiss()

      case Failure(t) =>
        // should not be failed since we used `recover`
        fail()
        w.dismiss()
    }

    w.await(limit)
  }

  test("sendToEurope can fail") {
    val w = new Waiter

    val europeConfirm: Future[Array[Byte]] = socket.sendToEurope(packet)

    europeConfirm.onComplete {
      case Success(p) =>
        p should be (expected)
        w.dismiss()
      case Failure(t: FailToSendException) =>
        t.url should be (Europe)
        w.dismiss()

      case _ => fail()
    }

    w.await(limit)
  }

}





