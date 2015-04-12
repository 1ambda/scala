package Akka

import akka.actor.{ActorSystem, Actor}
import akka.actor.{ActorRef, Props}

// ref: http://danielwestheide.com/blog/2013/02/27/the-neophytes-guide-to-scala-part-14-the-actor-approach-to-concurrency.html

/*
due to the non-blocking nature of actors, one thread can execute many actors – switching between them depending on which of them has messages to be processed.
 */
sealed trait CoffeeRequest
case object CappuccinoRequest extends CoffeeRequest
case object EspressoRequest extends CoffeeRequest

class Barista extends Actor {
  def receive = {
    case CappuccinoRequest => println("I have to prepare a cappuccino!")
    case EspressoRequest => println("Let's prepare an espresso.")
  }
}

object AkkaApp1 extends App {
  val system = ActorSystem("Baristar")
  val barista: ActorRef = system.actorOf(Props[Barista], "Barista")
  barista ! CappuccinoRequest
  barista ! EspressoRequest
  println("I ordered a cappuccino and an espresso")
  system.shutdown()
}
