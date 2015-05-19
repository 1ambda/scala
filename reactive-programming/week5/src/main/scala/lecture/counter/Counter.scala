package lecture.counter

import akka.actor.{ActorSystem, ActorRef, Props, Actor}

class Counter extends Actor {
  var count = 0

  def receive = {
    case "incr" => count += 1
    case "get" =>
      sender ! count
      context.stop(self)
  }
}

class Customer(counter: ActorRef) extends Actor {
  def receive = {
    case "start" =>
      counter ! "incr"
      counter ! "incr"
      counter ! "incr"
      counter ! "get"

    case count: Int =>
      println(s"count: $count")
      context.stop(self)
  }
}

object CounterApp extends App {
  val system = ActorSystem("CounterSystem")
  val counter = system.actorOf(Props[Counter], "Counter")
  val customer = system.actorOf(Props(classOf[Customer], counter), "Customer")

  customer ! "start"
}
