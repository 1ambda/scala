package lecture.crawler

import akka.actor._
import scala.concurrent.duration._

class Controller extends Actor with ActorLogging {
  import Controller._

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 5) {
    case _ : Exception => SupervisorStrategy.Restart
  }

  var cache = Set.empty[String] // url cache already retrieved

  context.setReceiveTimeout(10 seconds)

  def receive = {
    case Check(url, depth) =>
      log.debug("{} checking {}", depth, url)

      if (!cache(url) && depth > 0)
        context.watch(context.actorOf(Props(new Getter(url, depth - 1))))

      cache += url

    case Terminated(_) =>
      if (context.children.isEmpty) context.parent ! Result(cache)

    case ReceiveTimeout => context.children foreach context.stop
  }
}

object Controller {
  sealed trait ControllerEvent
  case class Check(url: String, depth: Int) extends  ControllerEvent
  case class Result(cache: Set[String]) extends ControllerEvent
  case object Timeout extends ControllerEvent
}
