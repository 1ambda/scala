package lecture

import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SocketSpec extends FunSuite with Matchers {
  import Server._

  test("blocking") {
    val socket = new Socket {}

    val packet = socket.readFromMemory()
    val result = socket.send(US, packet)

    packet.toList should be (List(1, 2, 3, 4))
    result.toList should be (List(5, 6, 7, 8))
  }
}


