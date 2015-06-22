package actorpath

import akka.actor.{Actor, ActorRef, Props}
import scala.concurrent.duration._

case object IdentifyGreeter

class ActorPaths extends Actor {
  import context.dispatcher
   val greeter: ActorRef = context.actorOf(Props[Greeter], "greeter")

   println(greeter)      // Actor[akka://Main/user/app/greeter#-2107470677]
   println(greeter.path) // akka://Main/user/app/greeter

   val resolver: ActorRef = context.actorOf(Props[Resolver], "resolver")

   context.system.scheduler.scheduleOnce(100 milliseconds, self, IdentifyGreeter)

   override def receive = {
     case IdentifyGreeter =>
       resolver ! Resolve(greeter.path)

     case Resolved(path, ref) =>
       println("Greeter ActorRef resolved: " + ref)
       ref ! Stop
       context.system.scheduler.scheduleOnce(100 milliseconds, self, IdentifyGreeter)

     case NotResolved(path) =>
       println("Greeter ActorRef not resolved: ")
       context.stop(self)

     case _ => /* do nothing */
   }
 }
