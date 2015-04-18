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




