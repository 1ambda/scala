// ref: http://danielwestheide.com

package Akka.CoffeeShop

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

sealed trait CoffeeRequest
case object EspressoRequest extends CoffeeRequest
case object CappuccinoRequest extends CoffeeRequest
case object ClosingTime
case class Bill(cents: Int)

class Barista extends Actor {

  var cappuccinoCount = 0
  var espressoCount   = 0

  def receive = {
    case CappuccinoRequest =>
      cappuccinoCount += 1
      println("prepare a cappuccino")
    case EspressoRequest =>
      sender ! Bill(200)
      espressoCount += 1
      println("prepare an espresso")

    case ClosingTime =>
      println("close coffee shop")
      println(s"cappuccino count: $cappuccinoCount")
      println(s"espresso count: $espressoCount")
      context.system.shutdown
  }
}

case object OrderEspresso

class Customer(barista: ActorRef) extends Actor {
  def receive = {
    case OrderEspresso => barista ! EspressoRequest
    case Bill(cents) => println(s"I have to pay $cents")
  }
}

object CoffeeShop extends App {
  val system = ActorSystem("CoffeeShop")
  val barista1: ActorRef = system.actorOf(Props[Barista], "Barista1")
  val barista2: ActorRef = system.actorOf(Props[Barista], "Barista2")
  val customer1: ActorRef = system.actorOf(Props(classOf[Customer], barista1), "Customer1")

  barista1 ! CappuccinoRequest
  customer1 ! OrderEspresso

  // ask
  // implicit val timeout = Timeout(2.second)
  // implicit val ec: ExecutionContext = system.dispatcher

  // val f: Future[Any] = barista2 ? EspressoRequest 
  // f.onSuccess {
  //   case Bill(cents) => println(s"I have to pay $cents")
  // }

  barista1 ! ClosingTime
}
