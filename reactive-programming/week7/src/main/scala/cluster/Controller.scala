package cluster

import scala.concurrent.duration._
import akka.actor._
import Controller._

class Controller extends Actor with ActorLogging {
  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 5) {
    case _ : Exception => SupervisorStrategy.Restart
  }

  var cache = Set.empty[String]
  context.setReceiveTimeout(10 seconds)

  override def receive: Receive = {
    case Check(url, depth) =>
      log.info("depth: {}, checking {}", depth, url)

      if (!cache(url) && depth > 0) {
        val getter = context.actorOf(Props(new Getter(url, depth - 1)))
        context.watch(getter)
        cache += url
      }

    case Terminated(_) =>
      if (context.children.isEmpty) context.parent ! Result(cache)

    case ReceiveTimeout =>
      context.children foreach context.stop
  }
}

object Controller {
  sealed trait ControllerEvent
  case class Check(url: String, depth: Int) extends ControllerEvent
  case class Result(cache: Set[String]) extends ControllerEvent
  case object Timeout extends ControllerEvent
}

