package lecture

import org.junit.runner.RunWith
import org.scalatest.concurrent.AsyncAssertions
import org.scalatest.time.{Span, Seconds}
import org.scalatest.{FunSuite, Matchers}
import org.scalatest.junit.JUnitRunner

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

@RunWith(classOf[JUnitRunner])
class ResilientSocketSpec extends FunSuite with Matchers with AsyncAssertions {

  val packet: Array[Byte]     = List(1, 2, 3, 4).map(_.toByte).toArray
  val expected: Array[Byte] = List(5, 6, 7, 8).map(_.toByte).toArray

  val limit = timeout(Span(2, Seconds))
  val socket = new ResilientSocket {}

  test("sendToRecover") {
    val w = new Waiter

    val confirm: Future[Array[Byte]] = socket.sendToRecover(packet)

    confirm.onComplete {
      case Success(p) =>
        p should be (expected)
        w.dismiss()

        // TODO
      case region: Array[Byte] =>
        region.toString should be ("USA")

      case _ => fail()
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
      case Failure(t: SendToFailException) =>
        t.region should be ("Europe")
        w.dismiss()

      case _ => fail()
    }

    w.await(limit)
  }

}





