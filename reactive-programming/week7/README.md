# Week 7

## Distributed Actors

The impact of network communication compared to in-process communication

- data sharing only by values
- lower bandwidth
- higher latency
- partial failure
- data corruption

Multiple processes on the same machine are quantitatively less impacted, but qualitatively the issues are the same.

Distributed computing breaks assumptions made by the synchronous programming model.

- Actor communication is asynchronous, one-way not guaranteed
- Actor encapsulation makes form the URI's path elements
- Actors are **Location Transparent**, hidden behind `ActorRef`

### Actor Path

Every actor system has an `Address` forming schema and authority of a hierarchical URI.

```scala
class actorpath.ActorPaths extends Actor {
  val ref: ActorRef = context.actorOf(Props[actorpath.Greeter], "greeter")

  println(ref)      // Actor[akka://Main/user/app/greeter#-2107470677]
  println(ref.path) // akka://Main/user/app/greeter

  override def receive = {
    case _ => /* do nothing */
  }

  context.stop(self)
}

class actorpath.Greeter extends Actor {
  override def receive = {
    case _ => /* do nothing */
  }
}
```

- `akka://Main`: authority
- `user/app/greeter`: path

Remote address example: `akka.tcp://HelloWorld@10.2.4.6:6565`

### ActorRef vs ActorPath

Actor names are unique within a parent can be reused.

- `ActorPath` is the full name, whether the actor exists or not
- `ActorRef` points to an actor which was started; an **incarnation**
- `ActorPath` can only optimistically send a message
- `ActorRef` can be watched
- `ActorRef` example: `akka://HelloWorld/user/greeter#43428347`

```scala
final case class Identify(messageId: Any) extends AutoReceivedMessage

final case class ActorIdentity(correlationId: Any, ref: Option[ActorRef]) {
  def getRef: ActorRef = ref.orNull
}
```

### Resolving ActorRef Example

```scala
case object Stop
case object IdentifyGreeter

class actorpath.ActorPaths extends Actor {
  import context.dispatcher
  val greeter: ActorRef = context.actorOf(Props[actorpath.Greeter], "greeter")

  println(greeter)      // Actor[akka://Main/user/app/greeter#-2107470677]
  println(greeter.path) // akka://Main/user/app/greeter

  val resolver: ActorRef = context.actorOf(Props[actorpath.Resolver], "resolver")

  context.system.scheduler.scheduleOnce(100 milliseconds, self, IdentifyGreeter)

  override def receive = {
    case IdentifyGreeter =>
      resolver ! actorpath.Resolve(greeter.path)

    case actorpath.Resolved(path, ref) =>
      println("actorpath.Greeter ActorRef resolved: " + ref)
      ref ! Stop
      context.system.scheduler.scheduleOnce(100 milliseconds, self, IdentifyGreeter)

    case actorpath.NotResolved(path) =>
      println("actorpath.Greeter ActorRef not resolved: ")
      context.stop(self)

    case _ => /* do nothing */
  }
}

case class actorpath.Resolve(path: ActorPath)
case class actorpath.Resolved(path: ActorPath, ref: ActorRef)
case class actorpath.NotResolved(path: ActorPath)

class actorpath.Resolver extends Actor {
  override def receive = {
    case actorpath.Resolve(path) =>
      context.actorSelection(path) ! Identify((path, sender))

    case ActorIdentity((path: ActorPath, client: ActorRef), Some(ref)) =>
      client ! actorpath.Resolved(path, ref)

    case ActorIdentity((path: ActorPath, client: ActorRef), None) =>
      client ! actorpath.NotResolved(path)
  }
}

class actorpath.Greeter extends Actor {
  override def receive = {
    case Stop => context.stop(self)
    case _ => /* do nothing */
  }
}
```

### Relative Actor Paths

- `context.actorSelection("child/grandchild")`
- `context.actorSelection("../sibling")`
- `context.actorSelection("/user/app")`
- `context.actorSelection("/user/controllers/*"`

## Actor Cluster

[Ref - Akka Cluster Usage](http://doc.akka.io/docs/akka/snapshot/java/cluster-usage.html)

### Seed Nodes

When a new node is started it sends a message to all seed nodes and then sends join command to the one that answers first. 
If no one of the seed nodes replied (might not be started yet) it retires the procedure until successful of shutdown.

```scala
// application.conf

akka.cluster.seed-nodes = [
  "akka.tcp://ClusterSystem@host1:2552",
  "akka.tcp://ClusterSystem@host2:2552"]
```

```java
// java system properties when starting the JVM
-Dakka.cluster.seed-nodes.0=akka.tcp://ClusterSystem@host1:2552
-Dakka.cluster.seed-nodes.1=akka.tcp://ClusterSystem@host2:2552
```

The seed nodes can be started in any order and it is not necessary to have all seed nodes running, 
but the node configured as the first element in the `seed-nodes` configuration list must be started 
when initially starting a cluster otherwise the other seed-nodes will not become initialized and no other node can join the cluster.

Once more than two seed nodes have been started it is no problem to shut down the first seed node. 
If the first seed node is restarted, it will first try to join the other seed nodes in the existing cluster.

If you don't configure seed nodes, you need to join the cluster programmatically or manually.

You can join to any node in the cluster. It does not have to be configured as a seed node. 
Note that you can only join to an existing cluster member, which means that for bootstrapping some node must join itself, 
and then the following nodes could join them to make up a cluster.

An actor system can only join a cluster once. Additional attempts will be ignored. When it has successfully joined 
it must be restarted to be able to join another cluster or to join the same cluster again. 
It can use the same host name and port after the restart, when it come up as new incarnation of existing member in the cluster 
, trying to join in, then the existing one will be removed from the cluster and then it will be allowed to join.

### Automatic vs. Manual Downing

When a member is considered by the failure detector to be unreachable the leader is not allowed to perform its duties, 
such as changing status of new joining members to `Up`. The node must first become reachable again, or the status of the unreachable member 
must be changed to `Down`. Changing status to `Down` can be performed automatically or manually. By default it must be done manually.

You can enable automatic downing with configuration.

``scala
akka.cluster.auto-down-unreachable-after = 120s
```

This means that the cluster leader member will change the **unreachable** node status to `down` automatically after the configured time of unreachability.

Be aware of that using auto-down implies that two separate clusters will automatically be formed in case of network partition. 
That might be desired by some applications but not by others.

### Leaving

There are two ways to remove a member from the cluster.

You can just stop the actor system (or JVM process). It will be detected as unreachable and removed after the automatic or manual downing.

A more graceful exit can be performed if you tell the cluster that a node shall leave. 
This can be performed using [JMX](http://doc.akka.io/docs/akka/snapshot/java/cluster-usage.html#cluster-jmx-java) or [CLI Management](http://doc.akka.io/docs/akka/snapshot/java/cluster-usage.html#cluster-command-line-java). 
It can also be performed programmatically with `Cluster.get(system).leave(address)`

The leaving member will be shutdown after the leader has changed status of the member to `Exiting`.

### Subscribe to Cluster Events

You can subscribe to change notification of the custer membership by using `Cluster.get(system).subscribe`

```scala
cluster.subscribe(getSelf(), MemberEvent.class, unreachableMember.class);
```

A snapshot of the full state, `akka.cluster.ClusterEvent.CurrentClusterState`, is sent to the subscriber as the first message, 
followed by events for incremental updates.

Note that you may receive an empty `CurrentClusterState`, containing no members, 
if you start the subscription before the initial join procedure has completed. This is expected behavior. 
When the node has been accepted in the cluster you will receive `MemberUp` for that node, and other nodes.

If you find it inconvenient to handle the `CurrentClusterState` you can use `ClusterEvent.initialStateAsEvents()` as 
parameter to subscribe. That means that instead of receiving `CurrentClusterState` as the first message 
you will receive the events corresponding to the current state to mimic what you would have seen if you were listening to 
the events when they occurred in the past. Note that those initial events only correspond to the current state and 
it is not the full history of all changes that actually has occurred in the cluster.

```scala
cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(), 
    MemberEvent.class, UnreachableMember.class);
```

### Life Cycle of Members

- `MemberUp`: a new member has joined the cluster and its status has been changed to `Up`
- `MemberExited`: a member is leaving the cluster and its status has been changed to `Exiting`. Note that 
the node might already have been shutdown when this event is published on another node
- `MemberRemoved`: a member completely removed from the cluster
- `UnreachableMember`: a member is considered as unreachable, detected by the failure detector of at least on other node
- `ReachableMember`: a member is considered as reachable again, after having been unreachable. All nodes that 
previously detected it as unreachable has detected it as reachable again.



















