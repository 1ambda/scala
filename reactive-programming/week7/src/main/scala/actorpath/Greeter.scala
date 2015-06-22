package actorpath

import akka.actor.Actor
case object Stop

class Greeter extends Actor {
  override def receive = {
    case Stop => context.stop(self)
    case _ => /* do nothing */
  }
}
