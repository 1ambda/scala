package lecture

import scala.concurrent.Future
import scala.util.Random
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global

case class FailToSendException(url: URL) extends RuntimeException
case class URL(url: String)

trait mailServer {
  val Europe = URL("mail.exchange.eu")
  val USA = URL("mail.exchange.us")
}


object Http extends Socket {

  class HttpMethod
  object HttpMethod {
    case object POST extends HttpMethod
    case object GET extends HttpMethod
    case object PUT extends HttpMethod
    case object DELETE extends HttpMethod
  }

  class HttpStatue
  object HttpStatus {
    case object isOK extends HttpStatue
  }

  case class Request(body: Array[Byte], method: HttpMethod)
  case class Response(body: Array[Byte], status: HttpStatue)

  def apply(url: URL, req: Request): Future[Response] = Future {
    sleepRandom
    Response(req.body.toList.map(x => (x + 4).toByte).toArray, HttpStatus.isOK)
  }
}

trait AsyncSocket extends Socket {

  def send(url: URL, packet: Array[Byte]): Future[Array[Byte]] = Future {
    sleepRandom
    val received = packet.toList
    // do something with packet
    received.map(x => (x + 4).toByte).toArray
  }
}

trait SyncSocket extends Socket {

  def send(url: URL, packet: Array[Byte]): Array[Byte] = {
    sleepRandom
    val received = packet.toList
    // do something with packet
    received.map(x => (x + 4).toByte).toArray
  }
}

trait Socket {

  def sleepRandom = Thread.sleep(Random.nextInt(500))

  def asyncReadFromMemory(): Future[Array[Byte]] = Future {
    sleepRandom
    List(1, 2, 3, 4).map(_.toByte).toArray
  }

  def syncReadFromMemory(): Array[Byte] = {
    sleepRandom
    List(1, 2, 3, 4).map(_.toByte).toArray
  }
}
