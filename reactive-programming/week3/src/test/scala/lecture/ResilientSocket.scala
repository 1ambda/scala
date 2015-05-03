package lecture

import scala.concurrent._
import scala.util._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.async.Async._
import scala.language.implicitConversions

trait ResilientSocket extends Socket with mailServer {
  import Http._

  def awaitRetry[T](times: Int)(block: => Future[T]): Future[T] = async {
    var i = 0
    var result: Try[T] = Failure(new RuntimeException("failure"))

    while (result.isFailure && i < times) {
      result = await { Try.convert(block) }
      i += 1
    }

    result.get
  }

  def foldLeftRetry[T](times: Int)(block: => Future[T]): Future[T] = {
    val failed: Future[T] = Future.failed(new RuntimeException("failed Future"))
    val blocks = (1 to times).map(_ => () => block)
    blocks.foldLeft(failed){
      (err, b) => err recoverWith { case _ => b() }
    }
  }

  def foldRightRetry[T](times: Int)(block: => Future[T]): Future[T] = {
    val failed: Future[T] = Future.failed(new RuntimeException("failed Future"))
    val blocks = (1 to times).map(_ => () => block)
    blocks.foldRight(() => failed) {
      (b, err) => () => { b() fallbackTo { err() }}
    }()
  }

  def recursiveRetry[T](times: Int)(block: => Future[T]): Future[T] = {
    if (times == 0) Future.failed(new RuntimeException("times can't be 0 in retry()"))
    else block fallbackTo {
      recursiveRetry(times - 1){ block }
    }
  }

  def sendToFallback(packet: Array[Byte]): Future[Array[Byte]] = {
    send(Europe, packet) fallbackTo {
      send(USA, packet)
    } recover {
      case t: FailToSendException =>
        // default value when two sending requests failed
        packet.toList.map(x => (x + 4).toByte).toArray
    }
  }

  def sendToRecover(packet: Array[Byte]): Future[Array[Byte]] = {
    send(Europe, packet) recoverWith {
      case FailToSendException(region) =>
        send(USA, packet) recover {
          case t: FailToSendException =>
            // default value when two sending requests failed
            packet.toList.map(x => (x + 4).toByte).toArray
        }
    }
  }

  def sendToEurope(packet: Array[Byte]): Future[Array[Byte]] =
    send(Europe, packet)

  def send(url: URL, packet: Array[Byte]): Future[Array[Byte]] =
    if (Random.nextBoolean() == true)
      url match {
        case Europe => Future.failed(FailToSendException(Europe))
        case USA => Future.failed(FailToSendException(USA))
      }
    else {
      Http(url, Request(packet, HttpMethod.POST))
        .filter(_.status == HttpStatus.isOK)
        .map(_.body)
    }
}

