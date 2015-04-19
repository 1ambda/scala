package Akka.EnhancedCoffeeShop

import akka.actor.{Actor, Props, ActorRef, ActorSystem, ActorLogging}
import concurrent.Future
import util.Random

// ref: http://danielwestheide.com/blog/2013/03/20/the-neophytes-guide-to-scala-part-15-dealing-with-failure-in-actor-systems.html

object Register {
  // sealed trait can be extended only in the same file as its declaration
  sealed trait Article
  case object Espresso extends Article
  case object Cappuccino extends Article 
  case class Transaction(article: Article)
  class PaperJamException(msg: String) extends Exception(msg)
}

class Register extends Actor {
  import Register._
  import Barista._

  var revenue = 0
  val prices = Map[Article, Int](
    Espresso -> 150,
    Cappuccino -> 250
  )

  def receive = {
    case Transaction(article) =>
      val price = prices(article)
      revenue += price

      // will throw a exception in about half of the cases
      // but no effect on our system. just the parent actor will be notified
      if (Random.nextBoolean())
        throw new PaperJamException("PaperJamException occured")

      sender ! Receipt(price)
  }
}

object Barista {
  case object EspressoRequest
  case object CappuccinoRequest
  case object ClosingTime

  case class Receipt(price: Int)

  case class Cup(state: Cup.State)
  object Cup {
    sealed trait State
    case object Clean extends State
    case object Filled extends State
    case object Dirty extends State
  }
}

class Barista extends Actor {
  import Register._
  import Barista._
  import Cup._

  import concurrent.duration._
  import akka.util.Timeout

  implicit val timeout = Timeout(4.seconds)
  val register = context.actorOf(Props[Register], "Register")

  // to use the same thread pool as our Barista actor use
  import context.dispatcher

  // akks patterns
  import akka.pattern.ask  // for `?`
  import akka.pattern.pipe // for 'pipeTo'

  def receive = {
    case EspressoRequest =>
      val receipt: Future[Any] = register ? Transaction(Espresso)
      receipt.map((Cup(Filled), _)).pipeTo(sender)

    case ClosingTime => context.stop(self)
  }
}

object Customer {
  case object EspressoOrder
}

class Customer(barista: ActorRef) extends Actor with ActorLogging {
  import Customer._
  import Barista._
  import Cup._

  def receive = {
    case EspressoOrder => barista ! EspressoRequest 

    case (Cup(Filled), Receipt(amount)) =>
      log.info(s"amount: $amount for $self")
  }
}

object EnhancedCoffeeShop extends App {
  import Customer._

  val system = ActorSystem("EnhancedCoffeeShop")
  val barista = system.actorOf(Props[Barista], "Barista")
  val customer1 = system.actorOf(Props(classOf[Customer], barista), "John")
  val customer2 = system.actorOf(Props(classOf[Customer], barista), "Marry")

  customer1 ! EspressoOrder
  customer2 ! EspressoOrder
}
