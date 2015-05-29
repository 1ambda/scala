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
    postStop() // stop the previous actor instance
  }

  def postRestart(
}
```
