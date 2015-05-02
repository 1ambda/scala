package lecture

import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SyncSocketSpec extends FunSuite with Matchers {

  test("blocking") {
    val socket = new SyncSocket {}

    val packet = socket.readFromMemory()
    val result = socket.send(socket.US, packet)

    packet.toList should be (List(1, 2, 3, 4))
    result.toList should be (List(5, 6, 7, 8))
  }
}


