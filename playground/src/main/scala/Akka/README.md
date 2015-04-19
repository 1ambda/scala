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

- `OneForOneStrategy`
- `AllForOneStrategy`



