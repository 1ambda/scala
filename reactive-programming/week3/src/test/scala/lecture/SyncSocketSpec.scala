package lecture

import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SyncSocketSpec extends FunSuite with Matchers with mailServer {

  test("blocking") {
    val socket = new SyncSocket {}

    val packet: Array[Byte] = socket.syncReadFromMemory()
    val result = socket.send(USA, packet)

    packet.toList should be (List(1, 2, 3, 4))
    result.toList should be (List(5, 6, 7, 8))
  }
}


