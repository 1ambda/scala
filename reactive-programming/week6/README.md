# Week 6

[Ref - Reactive Programming in Scala](https://class.coursera.org/reactive-002/)

- Failure Handling with Actors
- Lifecycle Monitoring and the Error Kernel
- Persistent Actor State

## Failure Handling with Actors

Resilience demands containment and delegation of failure

- failed Actor is terminated or restarted
- decision must be taken by one other Actor
- supervised Actors form a tree structure
- the supervisor need to create its subordinate

### Supervisor Strategy

The parent can declares how it child Actors are supervised in Akka.

```scala
class Manager extends Actor {
  override val supervisorStrategy = OneForOneStrategy() {
    case _: DBException          => Restart
    case _: ActorKilledException => Stop
    case _: ServiceDownException => Escalate
  }

  ...
  ...
  // children
  val db      = context.actorOf(Props[DBActor], "db")
  val service = context.actorOf(Props[ServiceActor], "service")
}
```

Failure is sent and processed like a message

```scala
Class Manager extends Actor {
  var restarts = Map.empty[ActorRef, Int].withDefaultValue(0)

  override val = supervisorStrategy = OneForOneStrategy() {
    case _: DBException =>
      restarts(sender) match {
        case toomany if toomany > 10 => restarts -= sender; Stop
        case n                       => restarts.updated(sender, n+1); Restart
      }
  }
}
```

If decision applies to all children,
We can use `AllForOneStrategy` that allow a finite number of restarts.
(also within a time window)

```scala
OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1.minute) {
 case : DBException => Restart // wil turn into Stop
}
```

### Actor, ActorRef

[Ref1 - Akka: Actor References, Paths and Addresses](http://doc.akka.io/docs/akka/snapshot/general/addressing.html)
[Ref2 - Akka: Actors](http://doc.akka.io/docs/akka/current/scala/actors.html)

#### What is an Actor Reference?

![](http://doc.akka.io/docs/akka/snapshot/_images/ActorPath.png)

> An actor reference is a subtype of `ActorRef`, whose foremost purpose is to support sending
messages to the actor it represents. Each actor has access to its canonical (local) reference
through the `self` field.

> This reference is also included as sender reference by default for all messages sent to other actors.  
Conversely, during message processing the actor has access to a reference representing the sender of the current
message through the `sender` method.

![](http://doc.akka.io/docs/akka/current/_images/actor_lifecycle1.png)

If an actor is stopped and a new one with the same name is created,
an `ActorRef` of old incarnation will not point to the new one.
Since an `ActorRef` always represent an incarnation (path and UID) not just a given path.

### Actor Life Cycle

![](https://raw.githubusercontent.com/1ambda/scala/master/reactive-programming/week6/screenshots/actor_life_cycle.png)

```scala
trait Actor {
  def preStart(): Unit = {}

  def preRestart(reason: Throwable, message: Option[Any]: Unit = {
    context.children foreach (context.stop(_))
    postStop() // stop the current actor instance
  }

  def postRestart(reason: Throwable): Unit = {
    preStart() // call pre start hook of this actor (newly created actor) 
  }
  
  def postStop(): Unit {}
}
```

postStop can be used to release resources. e.g

```scala
class DBActor extends Actor {
  val db = DB.openConnection(...)
  
  override def postStop(): Unit = { db.close() }
}
```

Actor-local state cannot be kept across restarts, only external state can be managed like this.

```scala
class Listener(source: ActorRef) extends Actor {
  override def preStart() { source ! RegisterListener(self) }
  override def preRestart(reason: Throwable, message: Option[Any]) {}
  override def postRestart(reason Throwable) {}
  override def postStop() { source ! UnregisterListener(self) }
}
```

Note that child actors not stopped during restart will be restarted recursively by context.

## Lifecycle Monitoring and The Error Kernel

- After stop, there will be no more responses
- No replies could also be due to communication failure.

To remove the ambiguity between an actor which has terminated and one which is just not replying anymore, 
there exists the feature called `DeathWatch`.

An `Actor` registers its interest using `context.watch(targetActor)`, 
it will receive a `Terminated(target)` message when target stops.

### DeathWatch API

```scala
trait ActorContext {
  def watch(target: ActorRef): ActorRef
  def unwatch(target: ActroRef): ActorRef
  
  case class Terminated private[akka] (actor: ActorReF)
    (val existenceConfirmed: Boolean, val addressTerminated: Boolean)
      extends AutoReceiveMessage with PossiblyHarmful
}
```

`AutoReceiveMessage` means, `Terminated` messages are handled by the context not an user.

![](https://raw.githubusercontent.com/1ambda/scala/master/reactive-programming/week6/screenshots/deathwatch.png)

### The Children List

Each actor maintains a list of the actors it created

- the child has been entered when `context.actorOf` returns
- the child has been removed when `Terminated` is received
- an actor name is available IFF there is no such child

```scala
trait Actor Context {
  def children: Iterable[ActorRef]
  def child(name: String): Option[ActorRef]
}
```

### Applying DeathWatch to Controller & Getter

#### Before

```scala
// Getter 
class Getter(url: String, depth: Int) extends Actor {
  import Getter._
  implicit val exec: ExecutionContextExecutor = context.dispatcher

  WebClient get url pipeTo self

  def receive = {
    case body: String =>
      for (link <- findLinks(body))
        context.parent ! Controller.Check(link, depth)

    case s: Status.Failure => stop()
    case Abort             => stop()
  }

  def stop() = {
    context.parent ! Done
    context.stop(self)
  }


  def findLinks(body: String) = {
    val document = Jsoup.parse(body, url)
    val links = document.select("a[href]")

    for (link <- links.iterator().asScala) yield link.absUrl("href")
  }
}
```

```scala
// Controller

class Controller extends Actor with ActorLogging {
  import Controller._

  var cache = Set.empty[String] // url cache already retrieved
  var children = Set.empty[ActorRef]

  context.setReceiveTimeout(10 seconds)

  def receive = {
    case Check(url, depth) =>
      log.debug("{} checking {}", depth, url)

      if (!cache(url) && depth > 0)
        children += context.actorOf(Props(new Getter(url, depth - 1)))

      cache += url

    case Getter.Done =>
      children -= sender
      if (children.isEmpty) context.parent ! Result(cache)

    case ReceiveTimeout => children foreach (_ ! Getter.Abort)
  }
}
```

#### After

```scala
// Controller
  def receive = {
    case Check(url, depth) =>
      log.debug("{} checking {}", depth, url)

      if (!cache(url) && depth > 0) 
        context.watch(context.actorOf(Props(new Getter(url, depth - 1))))
      
      cache += url

    case Terminated(_) =>
      if (context.children.isEmpty) context.parent ! Result(cache)

    case ReceiveTimeout => context.children foreach context.stop
  }
```

### Lifecycle Monitoring for Fail-Over

```scala
class Manager extends Actor {
  def prime(): Receive = {
    val db = context.actorOf(Props[DBActor], "primeDB")
    context.watch(db)
    
    { case Terminated("primeDB") => context.become(backUp())
  }
  
  def backup(): Receive = { ... }
  
  def receive prime() 
}
```

### The Error Kernel

> **Keep important data near the root, and delegate risk to the leaves**

- Avoid restarting `Actor`s with important state

#### Application to Receptionist

- Since `Receptionist` has `Jobs` which is important internal state, We should stop `Controller` if it has a problem.
 
#### Before

```scala
// class Receptionist

  def runNext(queue: Jobs): Receive = {
    reqNo += 1
    if (queue.isEmpty) waiting
    else {
      val controller = context.actorOf(Props[Controller], s"c$reqNo")
      controller ! Controller.Check(queue.head.url, 2)
      running(queue)
    }
  }

  def running(queue: Jobs): Receive = {
    case Controller.Result(links) =>
      val job = queue.head
      job.client ! Result(job.url, links)
      context.stop(sender)
      context.become(runNext(queue.tail))

    case Get(url) => context.become(enqueueJob(queue, Job(sender, url)))
  }
  
  ...
```

#### After

```scala
  override def supervisorStrategy = SupervisorStrategy.stoppingStrategy

  def runNext(queue: Jobs): Receive = {
    reqNo += 1
    if (queue.isEmpty) waiting
    else {
      val controller = context.actorOf(Props[Controller], s"c$reqNo")
      context.watch(controller) // added
      controller ! Controller.Check(queue.head.url, 2)
      running(queue)
    }
  }
  
  def running(queue: Jobs): Receive = {
    case Controller.Result(links) =>
      val job = queue.head
      job.client ! Result(job.url, links)
      context.stop(context.unwatch(sender))
      context.become(runNext(queue.tail))

    case Terminated(_) =>
      val job = queue.head
      job.client ! Failed(job.url)
      context.become(runNext(queue.tail))

    case Get(url) => context.become(enqueueJob(queue, Job(sender, url)))
  }
```

### Interjection: The EventStream

Actor can direct messages only at known addresses. 

The `EventStream` allow publication of messages to an unknown audience like broadcasting. 
Every actor can optionally subscribe to (part of) the `EventStream`

```scala
trait EventStream {
  def subscribe(subscriber: ActorRef, topic: Class[_]): Boolean
  def unsubscribe(subscriber: ActorRef, topic: Class[_]): Boolean
  def unsubscribe(subscriber: ActorRef): Boolean
  def publish(event: AnyRef): Unit
}
```

You can subscribe topics. For example,

```scala
class Listener extends Actor {
  context.system.eventStream.subscribe(self classOf[LogEvent])
  
  def = receive = {
    case e: LogEvent => ...
  }
  
  override def postStop(): Unit = {
    context.system.eventStream.unsubscribe(self)
  }
}
```

### Where do Unhandled Messages Go?
 
Since `Actor.Receive` is a partial function, the behavior may not apply. 
Every unhandled messages are passed into the `unhandled` method
 
```scala
trait Actor {
  ...
  
  def unhandled(message: Any): message match {
    case Terminated(target) => throw new DeathPactException(target)
    case msg => 
      context.system.eventStream.publish(UnhandledMessage(msg, sender, self))
  }
}
```

Unhandled `Terminated` messages result in `DeathPactException`.

<br/>

## Persistent Actor State

Actor representing a stateful resource

- shall not lose important state due to (system) failure
- must persist state as needed
- must recover state at (re)start

Two possibilities for persisting state

- in-place update. if actor updates, persistent location also updates.
- persist change in append only fashion (log)

### Changes vs Current State

Benefits of persisting current state

- Recovery of latest state in constant time
- Data volume depends on number of records, not their change rate.

Benefits of persisting changes

- History can be replayed, audited or restored
- Some processing errors can be corrected retroactively
- Additional insight can be gained on business processes
- Writing an append-only stream optimizes IO bandwidth
- Changes are immutable and can freely be replicated

### Snapshot

Immutable snapshots can be used to bound recovery time.

### Persistence Example

[Ref - Akka persistence, event sourcing in 30 minutes](http://www.slideshare.net/ktoso/akka-persistence-event-sourcing-in-30-minutes)

```scala
case class NewPost(text: String, id: Long)
case class BlogPosted(id: Long)
case class BlogNotPosted(id: Long, reason: String)

sealed trait Event
case class PostCreated(text: String) extends Event
case object QuotaReached             extends Event

case class State(posts: Vector[String], disabled: Boolean) {
  def updated(e: Event): State = e match {
    case PostCreated(text) => copy(posts = posts :+ text)
    case QuotaReached      => copy(disabled = true)
  }
}
```

```scala
class UserProcessor extends PersistentActor {

  var state = State(Vector.empty, false)

  def receiveCommand = {
    case NewPost(text, id) =>
      if (state.disabled) sender() ! BlogNotPosted(id, "quota reached")
      else {
        persist(PostCreated(text)) { e =>
          updateState(e)
          sender() ! BlogPosted(id)
        }

        persist(QuotaReached)(updateState)
      }
  }

  def updateState(e: Event) { state = state.updated(e) }
  def receiveRecover = { case e: Event => updateState(e) }
  ...
}
```

When this actor crashes, the state will be recovered using the `updateState` method.

### Persist vs PersistAsync

![](https://raw.githubusercontent.com/1ambda/scala/master/reactive-programming/week6/screenshots/persistAsync.png)

```scala
case new Post(text, id) => {
  if (!state.disabled) {
    val created = PostCreated(text)
    
    update(created)
    update(QuotaReached)
    
    persistAsync(created)(sender() ! BlogPosted(id))
    persisAsync(QuotaReached)(_ => ())
  } else sender() ! BlogNotPosted(id, "quota reached")
}
```

### At-Least-Once Delivery

- Guaranteeing delivery means retrying until successfully
- Retries are the sender's responsibility
- The recipient needs to acknowledge receipt
- Lost receipts lead to duplicate deliveries (at-least-once)

- Retrying means taking note that the message needs to be sent
- Acknowledgement means taking note of the receipt of the confirmation

```scala
// sender
class UserProcessor(publisher: ActorPath) extends PersistentActor with AtLeastOnceDelivery {

  def receiveCommand = {
    case NewPost(text, id) =>
      persist(PostCreated(text)) { e =>
        deliver(publisher, PublishPost(text, _))
        sender() ! BlogPosted(id) 
      }
    
    case PostPublished(id) => confirmDelivery(id)
  }
  
  def receiveRecover = { 
    case PostCreated(text) => deliver(publisher, PublishPost(text, _)) 
    case PostPublished(id) => confirmDelivery(id)
  }
}
```

The underscore passed as an argument of `PublishPost` is a Correlation ID(long integer number). 

It is important to note that delivery must be re-initiated after a crash

### Exactly-Once Delivery

- At-least-once delivery leads to duplicates at the receiver
- **At-least-once is the responsibility of the sender** 

- **Exactly-once is the responsibility of the recipient**
- The receiver need to remember what it has already done to avoid redoing it
 
```scala
// recipient
class Publisher extends PersistentActor {
  var expectedId = 0L
  
  def receiveCommand = {
    case PublishPost(text, id) =>
      if (id > expectedId) () // ignore, not yet ready for that
      else if (id < expectedId) sender() ! PostPublished(id)
      else persist(PostPublished(id)) { e =>
             sender() ! e
             // modify website
             expectedId += 1
           }
  }
  
  def receiveRecover = { case PostPublished(id) => expected = id + 1 }
}
```

### Important: When to Perform External Effect?

- Perform it before persisting for **at-least-once semantics**
- Perform it after persisting for at-most-once semantics

This choice need to be made based on the underlying business model.

If processing is **idempotent** then using at-least-once semantics achieves effectively **exactly-once processing**

### Summary

- Actor persist facts that represent changes to their state.
- Events can be replicated and used to inform other components
- Recovery replays past events to reconstruct state; snapshots reduce this cost.