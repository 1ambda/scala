package actorpath

import akka.actor._

case class NotResolved(path: ActorPath)
case class Resolved(path: ActorPath, ref: ActorRef)
case class Resolve(path: ActorPath)

class Resolver extends Actor {
   override def receive = {
     case Resolve(path) =>
       context.actorSelection(path) ! Identify((path, sender))

     case ActorIdentity((path: ActorPath, client: ActorRef), Some(ref)) =>
       client ! Resolved(path, ref)

     case ActorIdentity((path: ActorPath, client: ActorRef), None) =>
       client ! NotResolved(path)
   }
 }
