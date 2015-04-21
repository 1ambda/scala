# akka note

### Actor

[The Actor Approach to Concurrency](http://danielwestheide.com/blog/2013/02/27/the-neophytes-guide-to-scala-part-14-the-actor-approach-to-concurrency.html)

There is not a 1-to-1 relationship between an actor and a thread. Rather, one thread can execute many actors switching between them depending on which of them has messages to be processed.

```scala
class Barista extends Actor {
  def receive: PartialFunction[Any, Unit] = {
    case CappuccinoRequest => println("prepare a cappuccino")
    case EspressoRequest => println("prepare an espresso")
  }
}
```

1. The partial function returned by the `receive` method is responsible for processing your message.
2. When processing a message, an actor can do whatever you want it to, **apart from returning a value**
3. As the return type of `Unit` suggests, your partial function is **side-effecting**
4. Sending a message and processing it is done is an asynchronous and non-blocking fashion. The sender will not be blocked until the message has been processed by the receiver.

### Location Transparency

An `ActorRef` acts some kind of proxy to the actual actor. This is convinient because an `ActorRef` can be serialized, allowing it to be a proxy for a remote actor on some other machine. For the component aquiring an `ActorRef`, the location of the actor is compleltely transparent.

### Answering

```scala
def !(message: Any)(implicit sender: ActorRef = Actor.noSender): Unit

case obejct ClosingTime

val barista1: ActorRef = system.actorOf(Props[Barista], "Barista1")
barista1 ! ClosingTime
```

### sender

```scala
class Barista extends Actor {
  def receive = {
    case CappuccinoRequest => println("prepare a cappuccino")
    case EspressoRequest =>
      println("prepare an espresso")
      sender ! Bill(200)

    case ClosingTime =>
      context.system.shutdown
      println("close coffee shop")
  }
}

case object OrderEspresso

class Customer(barista: ActorRef) extends Actor {
  def receive = {
    case OrderEspresso => barista ! EspressoRequest
    case Bill(cents) => println("I have to pay $cents")
  }
}

val barista1: ActorRef = system.actorOf(Props[Barista], "Barista1")
val customer1: ActorRef = system.actorOf(Props(classOf[Customer], barista1), "Customer1")
```

### Ask Pattern

Use `?` instead of `!`. Generally, telling is preferable to asking.

```scala
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
implicit val timeout = Timeout(2.second)
implicit val ec: scala.concurrent.ExecutionContext = system.dispatcher

val f: Future[Any] = barista2 ? CappuccinoRequest

f.onSuccess {
  case Bill(cents) => println(s"Will pay $cents cents for a cappuccino")
}
```

### Stateful Actor

Since each message is processed in isolation, above code is similar to using `AtomicInteger` values in a non-actor environment

### Actor Hierarchies

Every single of your actors has got a parent actor, and that each actor can create child actors.

- *guardian actor* is the parent of all root-level user actors
- `akka.actor.ActorSystem` is not an actor itself

Each actor has `path`

```scala
barista1.path // => akka.actor.ActorPath = akka://Coffeehouse/user/Barista1
customer1.path // => akka.actor.ActorPath = akka://Coffeehouse/user/Customer1

// path can be used to look up another actor
context.actorSelection("../Barista1")
```

### Actor Specialization

[Ref: FP and Concurrent Patterns in Scala](http://www.slideshare.net/kellogh/houghton-082014fpconcurrencypatterns)

- **Actors:** Computation
- **Dispatchers:** Thread Pooling
- **Supervisors:** Reliability
- **Routers:** Concurrentcy

### Dispatchers

[Akka Doc: Dispatchers](http://doc.akka.io/docs/akka/snapshot/scala/dispatchers.html)

[Akka Essentials: Dispatcher](https://www.packtpub.com/books/content/dispatchers-and-routers)

All `MessageDispatcher` implementations are also an `ExecutionContext`, which means that they can be used to execute arbitrary code, for instance `Future`

> If you are in actor, use 'import context.dispatcher' to use the same thread pool as actor use. [Ref: Futures in Akka with Scala](http://www.nurkiewicz.com/2013/03/futures-in-akka-with-scala.html)

> Each actor is configured to be run on a `MessageDispatcher`, and that dispatcher doubles as an `ExecutionContext`. If the nature of the Future calls invoked by the actor matches or is compatible iwth the activities of tht actor (e.g all CPU bound and no latency requirements), then it may be easiest to reuse the dispatcher for running the Futures by importing `context.dispatcher` [Ref: Akka: Futures # Within Actors](http://doc.akka.io/docs/akka/snapshot/scala/futures.html#Within_Actors)

### Example App: Enhanced Coffee Shop

[Ref: The Actor Approach to Concurrency](http://danielwestheide.com/blog/2013/02/27/the-neophytes-guide-to-scala-part-14-the-actor-approach-to-concurrency.html)

```scala
package Akka.EnhancedCoffeeShop

import akka.actor.{Actor, Props, ActorRef, ActorSystem, ActorLogging}
import concurrent.Future

// ref: http://danielwestheide.com/blog/2013/03/20/the-neophytes-guide-to-scala-part-15-dealing-with-failure-in-actor-systems.html

object Register {
  // sealed trait can be extended only in the same file as its declaration
  sealed trait Article
  case object Espresso extends Article
  case object Cappuccino extends Article 
  case class Transaction(article: Article)
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
```

the output is

```scala
[INFO] [04/19/2015 11:42:31.617] [EnhancedCoffeeShop-akka.actor.default-dispatcher-2] [akka://EnhancedCoffeeShop/user/John] amount: 150 for Actor[akka://EnhancedCoffeeShop/user/John#-1651441006]
[INFO] [04/19/2015 11:42:31.617] [EnhancedCoffeeShop-akka.actor.default-dispatcher-7] [akka://EnhancedCoffeeShop/user/Marry] amount: 150 for Actor[akka://EnhancedCoffeeShop/user/Marry#404729533]
```

### Hanlding Crashes 

```scala
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
```

When an exception occured, it has no effect on our system. Just the parent actor will be notified. However, the exception in child actors is not handled in the parent actor's `receive` partial function. In Akka, these concers are clearly separated.


Each actor defines its own **supervisor strategy**, which tells Akks how to deal with certain types of erros occurring in your children. There are basically two different types of superviosr strategy.

- `OneForOneStrategy`: an error in one of your children will only affect the child actor from which the error originated
- `AllForOneStrategy`: an error will affect all of your child actors.

Whatever strategy you have, you need to specify a `Decider: PartialFunction[Throwable, Directive]` which describe how to deal with your problematic hild actor (or all your child actors)

### Directive

[akka-actor: FaultHandling.scala](https://github.com/akka/akka/blob/8485cd2ebb46d2fba851c41c03e34436e498c005/akka-actor/src/main/scala/akka/actor/FaultHandling.scala#L97)

```scala
sealed trait Directive
case object Resume extends Directive
case object Restart extends Directive
case object Stop extends Directive
case object Escalate extends Directive
```

- **Resume:** the child actor or all actors will simply resume processing messages as if nothing extraordinary had happened.
- **Restart:** create a new instance of your child actor or all actors. The reasoning behind this is that you assume the internal state of actor/actors is corrupted.
- **Stop:** kill the actor.
- **Esaclate:** delegate the decision about what to do to your own parent actor.

[Akka: defaultStrategy](https://github.com/akka/akka/blob/8485cd2ebb46d2fba851c41c03e34436e498c005/akka-actor/src/main/scala/akka/actor/FaultHandling.scala#L154)

```scala
final val defaultDecider: Decider = {
  case _: ActorInitializationException ⇒ Stop
  case _: ActorKilledException         ⇒ Stop
  case _: DeathPactException           ⇒ Stop
  case _: Exception                    ⇒ Restart
}

/**
 * When supervisorStrategy is not specified for an actor this
 * is used by default. OneForOneStrategy with decider defined in
 * [[#defaultDecider]].
 */
final val defaultStrategy: SupervisorStrategy = {
  OneForOneStrategy()(defaultDecider)
}
```

### Actor Life Cycle

you can *hook* at `preStart`, `postStop`, `preRestart`, `postRestart` methods. So if you wanna print log when `Register` actor restarting, override `preRestart` or `postRestart`

```scala
object Register {
  // sealed trait can be extended only in the same file as its declaration
  sealed trait Article
  case object Espresso extends Article
  case object Cappuccino extends Article 
  case class Transaction(article: Article)
  class PaperJamException(msg: String) extends Exception(msg)
}

class Register extends Actor with ActorLogging {
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

  override def postRestart(reason: Throwable) {
    super.postRestart(reason)

    log.info(s"Register actor restarted: ${reason.getMessage}")
  }
}
```

In short

- parent `Actor` handles childrens's exceptions
- hook methods can be inserted into the child actor error originated

### Restart N times

[Akka: Fault Tolerance](http://doc.akka.io/docs/akka/snapshot/scala/fault-tolerance.html)

```scala

import akka.actor.Actor
 
class Supervisor extends Actor {
  import akka.actor.OneForOneStrategy
  import akka.actor.SupervisorStrategy._
  import scala.concurrent.duration._
 
  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _: ArithmeticException      => Resume
      case _: NullPointerException     => Restart
      case _: IllegalArgumentException => Stop
      case _: Exception                => Escalate
    }
 
  def receive = {
    case p: Props => sender() ! context.actorOf(p)
  }
}
```

you can use `orElse` simplify strategy code. 

```scaka
  val baristaDecider: Decider = {
    case _: PaperJamException => Restart 
  }

  override val supervisorStrategy =
    OneForOneStrategy()(
      baristaDecider.orElse(SupervisorStrategy.defaultStrategy.decider))
```

### Error Kernel Pattern

- if an actor carries important internal state
- then it should delegate dangeours tasks to child actors
- so as to prevent that state-carrying actor from crashing!

for example, below code is not safe because the `Register` actor which carrying `revenue` state might crash.

```scala
class Register extends Actor with ActorLogging {
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
      if (Random.nextBoolean())
        throw new PaperJamException("PaperJamException occured")

      sender ! Receipt(price)
  }
}
```

Let's apply kernel pattern into our `Register` actor.

```scala
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
```

We used the default supervisor strategy to have the printer actor restart upon failure.

```scala
val customer = sender
(printer ? PrintJob(price)).map((customer, _)).pipeTo(self)
```

> Assigning the sender to a val is necessary for similar reasons: When mapping a future, we are no longer in the context of our actor either – since sender is a method, it would now likely return the reference to some other actor that has sent us a message, not the one we intended.

### Timeouts

When an exception occurs in the `ReceiptPrinter`, this leads to an `AskTimeoutException` which comes back to the `Barista` actor in an unsuccessfully completed `Future`

Since `map` to `Future` is success-biased, the customer piped will also receiv a `Failure` containing an `AskTimeoutException`. To resolve this, let's recover by mapping `CombackLater` messages.


```scala
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

  def receive = {
    case EspressoOrder => barista ! EspressoRequest

    case (Cup(Filled), Receipt(amount)) =>
      log.info(s"amount: $amount for $self")

    case CombackLater =>
      log.info("OMG! Comback Later")
  }
}
```

### Watch

you can **watch** other actors which is supervised by your actors.

```scala
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
```
