package cluster

import akka.actor.{ActorLogging, Actor}
import akka.actor.Actor.Receive
import Controller._

class Controller extends Actor with ActorLogging {
  override val supervisorStrategy


  override def receive: Actor.Receive = ???
}

object Controller {
  sealed trait ControllerEvent
  case class Check(url: String, depth: Int) extends ControllerEvent
  case class Result(cache: Set[String]) extends ControllerEvent
  case object Timeout ControllerEvent
}
