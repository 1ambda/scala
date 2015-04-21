package Akka.EnhancedCoffeeShop

import akka.actor.{Actor, Props, ActorRef, ActorSystem, ActorLogging}
import concurrent.Future
import scala.util.Failure
import util.Random
import concurrent.duration._
import akka.util.Timeout
import akka.pattern.ask  // for `?`
import akka.pattern.pipe // for 'pipeTo'

// actor exception hanlding policy
import akka.actor.{OneForOneStrategy, SupervisorStrategy}
import akka.actor.SupervisorStrategy._ // for directive



// ref: http://danielwestheide.com/blog/2013/03/20/the-neophytes-guide-to-scala-part-15-dealing-with-failure-in-actor-systems.html

object Register {
  // sealed trait can be extended only in the same file as its declaration
  sealed trait Article
  case object Espresso extends Article
  case object Cappuccino extends Article 
  case class Transaction(article: Article)
}

class Register extends Actor with ActorLogging {
  import Register._
  import Barista._
  import ReceiptPrinter._

  var revenue = 0
  val prices = Map[Article, Int](
    Espresso -> 150,
    Cappuccino -> 250
  )

  val printer = context.actorOf(Props[ReceiptPrinter], "Printer")
  import context.dispatcher
  implicit val timeout = Timeout(4 seconds)

  def receive = {
    case Transaction(article) =>
      val price = prices(article)

      // very important!
      // since sender is a method, it will return a different actor
      // in a future 
      val customer = sender
      (printer ? PrintJob(price)).map((customer, _)).pipeTo(self)

    case (customer: ActorRef, Receipt(price)) =>
      revenue += price
      customer ! Receipt(price)
  }

  override def postRestart(reason: Throwable) {
    super.postRestart(reason)
    log.info(s"restarted, revenue: $revenue")
  }
}

object ReceiptPrinter {
  case class PrintJob(amount: Int)
  class PaperJamException(msg: String) extends Exception(msg)
}

class ReceiptPrinter extends Actor with ActorLogging {
  import ReceiptPrinter._
  import Barista._
  var paperJam = false;

  def receive = {
    case PrintJob(amount) => sender ! createReceipt(amount)
  }

  def createReceipt(amount: Int): Receipt = {
    // will throw a exception in about half of the cases
    if (Random.nextBoolean()) paperJam = true
    if (paperJam) throw new PaperJamException("PaperJamException occured")
    Receipt(amount)
  }

  override def postRestart(reason: Throwable) {
    super.postRestart(reason)
    log.info(s"restarted. paperJam: $paperJam")
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
  import Customer._

  implicit val timeout = Timeout(4.seconds)
  val register = context.actorOf(Props[Register], "Register")

  // to use the same thread pool as our Barista actor use
  import context.dispatcher

  def receive = {
    case EspressoRequest =>
      val receipt: Future[Any] = register ? Transaction(Espresso)
      receipt.map((Cup(Filled), _)).recover {
        case _: akka.pattern.AskTimeoutException => CombackLater
      }.pipeTo(sender)

    case ClosingTime => context.stop(self)
  }
}

object Customer {
  case object EspressoOrder
  case object CombackLater
}

class Customer(barista: ActorRef) extends Actor with ActorLogging {
  import Customer._
  import Barista._
  import Cup._

  context.watch(barista)

  def receive = {
    case EspressoOrder => barista ! EspressoRequest

    case (Cup(Filled), Receipt(amount)) =>
      log.info(s"amount: $amount for $self")

    case CombackLater =>
      log.info("OMG! comback later")

    case akka.actor.Terminated(barista) =>
      log.info("OMG, let's find another coffeehouse")
  }
}

object EnhancedCoffeeShop extends App {
  import Customer._
  import Barista._

  val system = ActorSystem("EnhancedCoffeeShop")
  val barista = system.actorOf(Props[Barista], "Barista")
  val customer1 = system.actorOf(Props(classOf[Customer], barista), "John")
  val customer2 = system.actorOf(Props(classOf[Customer], barista), "Marry")

  customer1 ! EspressoOrder
  customer2 ! EspressoOrder

  barista ! ClosingTime
}
