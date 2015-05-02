package lecture

import scala.concurrent.Future
import scala.util.Random
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global

case class SendToFailException(region: String) extends RuntimeException
trait mailServer {
  import Http._
  val Europe = URL("mail.exchange.eu")
  val USA = URL("mail.exchange.us")
}

trait ResilientSocket extends Socket with mailServer {

  import Http._

  def readFromMemory(): Future[Array[Byte]] = Future {
    sleepRandom
    List(1, 2, 3, 4).map(_.toByte).toArray
  }

  def sendToRecover(packet: Array[Byte]): Future[Array[Byte]] = {
    sendTo(Europe, packet) recoverWith {
      case SendToFailException(region) =>
        sendTo(USA, packet) recover {
          case t: SendToFailException => t.region.map(_.toByte).toArray
        }
    }
  }

  def sendToEurope(packet: Array[Byte]): Future[Array[Byte]] =
  sendTo(Europe, packet)

  def sendTo(url: URL, packet: Array[Byte]): Future[Array[Byte]] =
    if (Random.nextBoolean() == true)
      url match {
        case Europe => Future.failed(SendToFailException("Europe"))
        case USA => Future.failed(SendToFailException("USA"))
      }
    else {
      Http(url, Request(packet, HttpMethod.POST))
        .filter(_.status == HttpStatus.isOK)
        .map(_.body)
    }
}

object Http extends Socket {

  case class URL(url: String)

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

  def readFromMemory(): Future[Array[Byte]] = Future {
    sleepRandom
    List(1, 2, 3, 4).map(_.toByte).toArray
  }

  def send(to: Server, packet: Array[Byte]): Future[Array[Byte]] = Future {
    sleepRandom
    val received = packet.toList
    // do something with packet
    received.map(x => (x + 4).toByte).toArray
  }
}

trait SyncSocket extends Socket {

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

trait Socket {
  trait Server
  case object Europe extends Server
  case object US extends Server
  def sleepRandom = Thread.sleep(Random.nextInt(500))
}
