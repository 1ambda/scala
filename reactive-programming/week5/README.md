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



### Testing Actors



### Summary

- Actors are fully encapsulated, independent agents of computation
- Messages are the only way to interact with actors
- Explicit messaging allow explicit treatment of reliability
- The order in which messages are processed is mostly undefined