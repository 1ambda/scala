package lecture

import scala.concurrent.Future
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global

trait Socket {
  import Server._
  import Sleep._

  def readFromMemory(): Array[Byte] = {
    sleepRandom
    List(1, 2, 3, 4).map(_.toByte).toArray
  }

  def send(to: Server, packet: Array[Byte]): Array[Byte] = {
    sleepRandom
    val received = packet.toList
    // do something with packet
    received.map(x => (x + 4).toByte).toArray
  }
}

object Server {
  trait Server
  case object Europe extends Server
  case object US extends Server
}

object Sleep {
  def sleepRandom = Thread.sleep(Random.nextInt(500))
}


trait AsyncSocket {
  import Server._
  import Sleep._

  def readFromMemoryAsync(): Future[Array[Byte]] = Future {
    sleepRandom
    List(1, 2, 3, 4).map(_.toByte).toArray
  }

  def sendAsync(to: Server, packet: Array[Byte]): Future[Array[Byte]] = Future {
    sleepRandom
    val received = packet.toList
    // do something with packet
    received.map(x => (x + 4).toByte).toArray
  }
}
