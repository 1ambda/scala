package cluster

import akka.actor.Actor.Receive
import akka.actor._
import akka.remote.RemoteScope
import cluster.Controller.Check

import scala.concurrent.duration._


class Customer(client: ActorRef, url: String, node: Address) extends Actor {

  implicit val s = context.parent

  override val supervisorStrategy = SupervisorStrategy.stoppingStrategy

  val props = Props[Controller].withDeploy(Deploy(scope = RemoteScope(node)))
  val controller = context.actorOf(props, "controller")

  context.watch(controller)

  context.setReceiveTimeout(5 seconds)

  controller ! Check(url, 2)

  override def receive: Actor.Receive = ???
}
