## Week5, Actor

### Using `become`

```scala
class Counter extends Actor {
  def counter(n: Int): Receive = {
    case "incr" => context.become(counter(n + 1))
    case "get"  => sender ! n
  }
}

def receive = counter(0)
```

Funtionally equivalent to previous version with advantages

- state change is explicit
- state is scoped to current behavior

### Props

[Akka Source - Props](https://github.com/1ambda/akka/blob/master/akka-actor/src/main/scala/akka/actor/Props.scala)

- `Props` is a `ActorRef` configuration object, that is immutable, so it is thread safe and fully sharable.

```scala
// Props.scala

def apply(clazz: Class[_], args: Any*): Props = apply(defaultDeploy, clazz, args.toList)
```

### ActorRef

[Akka Source - ActorRef](https://github.com/1ambda/akka/blob/master/akka-actor/src/main/scala/akka/actor/ActorRef.scala)

> `ActorRef` is a immutable and serializable handle to an actor, which may or may not reside on the local host or inside the same `ActorSytem`. `ActorRef`s can be freely shared among actors by message passing.

### Actor

[Akka Source - Actor](https://github.com/1ambda/akka/blob/master/akka-actor/src/main/scala/akka/actor/Actor.scala)

**Actors are completely independent agents of computation**.

- local execution, no notion of global synchronization
- all actors run fully concurrently
- message-passing primitive is one-way communication

**An Actor is effectively single-threaded**

- messages are received sequentially
- behavior change is effective before processing the next message
- processing one message is atomic unit of execution

**Blocking is replaced by enqueuing a message**

### Example Actor Application

```scala
class Counter extends Actor {
  var count = 0
  
  def receive = {
    case "incr" => count += 1
    case ("get", customer: ActorRef) => customer ! count
  }
}

class Main extends Actor {
  val counter = context.actorOf(Props[Counter], "Counter")

  counter ! "incr"
  counter ! "incr"
  counter ! "incr"
  counter ! "get"

  def receive = {
    case count: Int =>
      println(s"count was $count")
      context.stop(self)
  }
}
```

### LoggingReceive

[Akka Source - Logging Receive](https://github.com/akka/akka/blob/master/akka-actor/src/main/scala/akka/event/LoggingReceive.scala)

> `LoggingReceive` wraps a receive partial function in a logging enclosure, which sends
> a debug message to the event bug each time before a message is matched.

### Bank Account Example

```scala
// TransferApp.scala
package lecture.bank

import akka.actor.{Actor, Props}
import akka.event.LoggingReceive

class TransferApp extends Actor {
  val accountA = context.actorOf(Props[BankAccount], "accountA")
  val accountB = context.actorOf(Props[BankAccount], "accountB")

  accountA ! BankAccount.Deposit(100)

  def receive = LoggingReceive {
    case BankAccount.Done => transfer(150)
  }

  def transfer(amount: BigInt): Unit = {
    val transaction = context.actorOf(Props[WireTransfer], "transfer")
    transaction ! WireTransfer.Transfer(accountA, accountB, amount)

    context.become(LoggingReceive {
      case WireTransfer.Done =>
        println("success")
        context.stop(self)

      case WireTransfer.Failed =>
        println("failed")
        context.stop(self)
    })
  }
}
```

```scala
// BankAccount.scala
package lecture.bank

import akka.actor.Actor
import akka.event.LoggingReceive

class BankAccount extends Actor {
  import BankAccount._

  var balance = BigInt(0)

  def receive = LoggingReceive {
    case Deposit(amount) =>
      balance += amount
      sender ! Done

    case Withdraw(amount) if amount <= balance =>
      balance -= amount
      sender ! Done

    case _ =>
      sender ! Failed
  }
}

object BankAccount {
  case class Deposit(amount: BigInt) {
    require (amount > 0)
  }

  case class Withdraw(amount: BigInt) {
    require (amount > 0)
  }

  case object Done
  case object Failed
}

```

```scala
// WireTransfer.scala
package lecture.bank

import akka.actor.{Actor, ActorRef}

class WireTransfer extends Actor {
  import WireTransfer._

  def receive = {
    case Transfer(from, to, amount) =>
      from ! BankAccount.Withdraw(amount)
      context.become(awaitWithdraw(to, amount, sender))
  }

  def awaitWithdraw(to: ActorRef, amount: BigInt, client: ActorRef): Receive = {
    case BankAccount.Done =>
      to ! BankAccount.Deposit(amount)
      context.become(awaitDeposit(client))

    case BankAccount.Failed =>
      client ! Failed
      context.stop(self)
  }

  def awaitDeposit(client: ActorRef): Receive = {
    case BankAccount.Done =>
      client ! Done
      context.stop(self)
  }
}

object WireTransfer {

  case class Transfer(from: ActorRef, to: ActorRef, amount: BigInt)
  case object Done
  case object Failed
}
```

```scala
// sbt "run-main akka.Main lecture.bank.TransferApp" -Dakka.loglevel=DEBUG -Dakka.actor.debug.receive=on

[info] Running akka.Main lecture.bank.TransferApp
[DEBUG] [05/19/2015 19:57:23.397] [run-main-0] [EventStream(akka://Main)] logger log1-Logging$DefaultLogger started
[DEBUG] [05/19/2015 19:57:23.397] [run-main-0] [EventStream(akka://Main)] Default Loggers started
[DEBUG] [05/19/2015 19:57:23.406] [Main-akka.actor.default-dispatcher-2] [akka://Main/user/app/accountA] received handled message Deposit(100)
[DEBUG] [05/19/2015 19:57:23.406] [Main-akka.actor.default-dispatcher-3] [akka://Main/user/app] received handled message Done
[DEBUG] [05/19/2015 19:57:23.407] [Main-akka.actor.default-dispatcher-3] [akka://Main/user/app/accountA] received handled message Withdraw(150)
[DEBUG] [05/19/2015 19:57:23.408] [Main-akka.actor.default-dispatcher-4] [akka://Main/user/app] received unhandled message Failed
```

### Reliable Messaging

- all messages can be persisted
- can include unique correlation IDs
- delivery can be retries until successful

> Reliability can only be ensured by business-level acknowledgement

How to we apply these principles in our bank account transfer example?

- log activities of `WireTransfer` to persistent storage
- each transfer has a unique ID
- add ID to `Withdraw` and `Deposit`
- store IDs of completed actions within `BankAccount`

> If an actor send multiple messages to the same destination, they will not arrive out of order 
> (this is Akka-specific) 

### Designing Actor Systems

#### Getter

```scala
class Client {
  import Client._

  val client = new AsyncHttpClient
  def getBlocking(url: String): String = {

    /* will be blokcing */
    val res = client.prepareGet(url).execute().get

    if (res.getStatusCode < 400)
      res.getResponseBodyExcerpt(131072 /* 128KB */)
    else throw BadStatus(res.getStatusCode)
  }

  def get(url: String)(implicit exec: Executor): Future[String] = {

    // java future
    val f = client.prepareGet(url).execute()
    // scala promise
    val p = Promise[String]()

    f.addListener(new Runnable {
      override def run(): Unit = {
        val res = f.get

        if (res.getStatusCode < 400) p.success(res.getResponseBodyExcerpt(131072))
        else p.failure(BadStatus(res.getStatusCode))
      }
    }, exec)

    p.future
  }
}

object Client {
  case class BadStatus(statusCode: Int) extends RuntimeException
}
```

> A reactive application is non-blocking & event-driven top to bottom 

#### Akka.pattern `pipeTo`

```scala
implicit val exec: ExecutionContextExecutor = context.dispatcher

val f: Future[String] = WebClient.get(url)

f.onComplete {
  case Success(body) => self ! body
  case Failure(t)    => self ! Status.Failure(t)
}
```

simply 

```scala
f.pipeTo(self)

// or just
WebClient get url pipeTo self
```

#### Getter

```scala
class Getter(url: String, depth: Int) extends Actor {
  implicit val exec: ExecutionContextExecutor = context.dispatcher

  WebClient get url pipeTo self

  // same as
  // val f: Future[String] = WebClient.get(url)
  // f.onComplete {
  //   case Success(body) => self ! body
  //   case Failure(t)    => self ! Status.Failure(t)
  // }

  def receive = {
    case body: String =>
      for (link <- findLinks(body))
        context.parent ! Controller.check(link, depth)

    case _: Status.Failure =>
      context.parent ! Done
  }

  def findLinks(body: String): Unit = {
    val document = Jsoup.parse(body, url)
    val links = document.select("a[href]")

    for (link <- links.iterator().asScala) yield link.absUrl("href")
  }
}

object Getter {
  sealed trait GetterEvent
  case object Done extends GetterEvent
}
```

#### ActorLogging

`ActorLogging` provides a logger

[Akka Docs - How to Log](http://doc.akka.io/docs/akka/snapshot/scala/logging.html#How_to_Log)

```scala
class MyActor extends Actor {
  val log = Logging(context.system, this)
  override def preStart() = {
    log.debug("Starting")
  }
  ...
  ...
  
// same as 
class MyActor extends Actor with akka.actor.ActorLogging {
 ...
 ...
```

#### Controller

```scala
class Controller extends Actor with ActorLogging {
  import Controller._

  var cache = Set.empty[String] // url cache already retrieved
  var chdilren = Set.empty[ActorRef]

  def receive = {
    case Check(url, depth) =>
      log.debug("{} checking {}", depth, url)

      if (!cache(url) && depth > 0)
        chdilren += context.actorOf(Props(new Getter(url, depth - 1)))

      cache += url

    case Getter.Done =>
      chdilren -= sender
      if (chdilren.isEmpty) context.parent ! Result(cache)
  }
}

object Controller {
  sealed trait ControllerEvent
  case class Check(url: String, depth: Int) extends  ControllerEvent
  case class Result(cache: Set[String]) extends ControllerEvent
}
```

#### Handling Timeout

The receive timeout is reset by every received message

```scala
class Controller extends Actor with ActorLogging {
  context.setReceiveTimeout(10 seconds)
  
  def receive = {
    case ReceiveTimeout => children foreach (_ ! Getter.Abort)
  }
  
// Getter
def receive {
  case Abort => ... 
}
```

#### Scheduler

Akka includes a timer service optimized for high volume, short durations and frequent cancellation

```scala
trait Scheduler {
  def scheduleOnce(delay: FiniteDuration, target: ActorRef, msg: Any)
        (implicit ec: ExecutionContext): Cancellable
}
```

Using `Scheduler, we can modify the previous code like

```scala
class Controller extends Actor with ActorLogging {
  import Controller._

  var cache = Set.empty[String] // url cache already retrieved
  var chdilren = Set.empty[ActorRef]

  context.system.scheduler.scheduleOnce(10.seconds) {
    chdilren foreach (_ ! Getter.Abort)
  }
```

But this code is problematic since the block pass as the parameter of `scheduleOnce` function 
runs on outside of `Actor` context. So it is not **thread-safe**

We should fix the above code like

```scala
class Controller extends Actor with ActorLogging {
  import Controller._

  var cache = Set.empty[String] // url cache already retrieved
  var chdilren = Set.empty[ActorRef]

  context.system.scheduler.scheduleOnce(10.seconds, self, Timeout) 

  def receive = {
    case Check(url, depth) =>
      log.debug("{} checking {}", depth, url)

      if (!cache(url) && depth > 0)
        chdilren += context.actorOf(Props(new Getter(url, depth - 1)))

      cache += url

    case Getter.Done =>
      chdilren -= sender
      if (chdilren.isEmpty) context.parent ! Result(cache)

    case Timeout => chdilren foreah (_ ! Getter.Abort)
  }
}

object Controller {
  sealed trait ControllerEvent
  case class Check(url: String, depth: Int) extends  ControllerEvent
  case class Result(cache: Set[String]) extends ControllerEvent
  case object Timeout extends ControllerEvent
}
```

This code is also problematic. Do not refer to actor state from code running asynchronously 

```scala
  def receive = {
    case Get(url) =>
      if (cache contains url) sender ! cache(url)
      else
        WebClient get url foreach { body => 
          cache += url -> body
          sender ! body
        }
  }
  
// correct way
def receive = {
  case Get(url) =>
    if (cache contains url) sender ! cache(url)
    else {
      val client: ActorRef = sender /* sender might be different from origin in the map function*/
      WebClient get(url) map(Result(client, url, _)) pipeTo self
    }

  case Result(client, url, body) =>
    cache += url -> body
    client ! body
}
```

### Receptionist

```scala
class Receptionist extends Actor {
  import Receptionist._

  var reqNo = 0
  def runNext(queue: Jobs): Receive = {
    reqNo += 1
    if (queue.isEmpty) waiting
    else {
      val controller = context.actorOf(Props[Controller], s"c$reqNo")
      controller ! Controller.Check(queue.head.url, 2)
      running(queue)
    }
  }

  def enqueueJob(queue: Jobs, job: Job): Receive = {
    if (queue.size > 3) {
      sender ! Failed(job.url)
      running(queue)
    } else running(queue :+ job)
  }

  def receive = waiting

  val waiting: Receive = {
    case Get(url) => context.become(runNext(Vector(Job(sender, url))))
  }

  def running(queue: Jobs): Receive = {
    case Controller.Result(links) =>
      val job = queue.head
      job.client ! Result(job.url, links)
      context.stop(sender)
      context.become(runNext(queue.tail))

    case Get(url) => context.become(enqueueJob(queue, Job(sender, url)))
  }
}

object Receptionist {
  case class Job(client: ActorRef, url: String)
  type Jobs = Vector[Job]

  sealed trait ReceptionistEvent
  case class Failed(url: String) extends ReceptionistEvent
  case class Get(url: String)    extends ReceptionistEvent
  case class Result(url: String, links: Set[String]) extends ReceptionistEvent
}
```

#### Main

```scala
// command
// sbt "run-main akka.Main lecture.crawler.CrawlerApp" -Dakka.loglevel=DEBUG -Dakka.actor.debug.receive=on

package lecture.crawler

import akka.actor.{ReceiveTimeout, Actor, Props}

import scala.concurrent.duration._

class CrawlerApp extends Actor {

  val receptionist = context.actorOf(Props[Receptionist], "receptionist")

  receptionist ! Receptionist.Get("http://www.google.com")

  context.setReceiveTimeout(20 seconds)

  def receive = {
    case Receptionist.Result(url, links) =>
      println(links.toVector.sorted.mkString(s"Results for '$url':\n", "\n", "\n"))

    case Receptionist.Failed(url) =>
      println(s"Failed to fetch '$url'\n")

    case ReceiveTimeout =>
      context.stop(self)
  }

  override def postStop(): Unit = {
    WebClient.shutdown()
  }
}

// response
Results for 'http://www.google.com':
http://maps.google.co.kr/maps?hl=ko&tab=wl
http://news.google.co.kr/nwshp?hl=ko&tab=wn
http://www.google.co.kr/?gfe_rd=cr&ei=eepdVbb4FMyT8QfY44CgBA
http://www.google.co.kr/advanced_search?hl=ko&authuser=0
http://www.google.co.kr/chrome/index.html?hl=ko&brand=CHNG&utm_source=ko-hpp&utm_medium=hpp&utm_campaign=ko
http://www.google.co.kr/history/optout?hl=ko
http://www.google.co.kr/imghp?hl=ko&tab=wi
http://www.google.co.kr/intl/ko/about.html
http://www.google.co.kr/intl/ko/ads/
http://www.google.co.kr/intl/ko/options/
http://www.google.co.kr/intl/ko/policies/privacy/
http://www.google.co.kr/intl/ko/policies/terms/
http://www.google.co.kr/intl/ko/services/
http://www.google.co.kr/language_tools?hl=ko&authuser=0
http://www.google.co.kr/preferences?hl=ko
http://www.google.co.kr/setprefdomain?prefdom=US&sig=0_lK0Z9J8V5G23lhASnQ7ePJ38nOo%3D
http://www.google.com
http://www.youtube.com/?gl=KR&tab=w1
https://accounts.google.com/ServiceLogin?hl=ko&continue=http://www.google.co.kr/%3Fgfe_rd%3Dcr%26ei%3DeepdVbb4FMyT8QfY44CgBA
https://drive.google.com/?tab=wo
https://mail.google.com/mail/?tab=wm
https://play.google.com/?hl=ko&tab=w8
https://plus.google.com/102197601262446632410
```

### Testing Actors

- Tests can only verify externally observable effects.




### Summary

- Actors are fully encapsulated, independent agents of computation
- Messages are the only way to interact with actors
- Explicit messaging allow explicit treatment of reliability
- The order in which messages are processed is mostly undefined
- Actors are run by a dispatcher (potentially shared) which can also run `Futuer`s

- Prefer immutable data structures, since they can be shared
- Prefer `context.become` for different states, with data local to the behavior
- A reactive application is non-blocking & event-driven top to bottom
- Do not refer to actor stats from code running asynchronously 

- Tests can only verify externally observable effects.