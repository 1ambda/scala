# Week 6 Assignment

## Implementation

### Step 5

Implement the use of persistence and replication at the primary replica.

```scala
```

### Step 4

Implement the use of persistence at the secondary replicas.

```scala
class Replica(val arbiter: ActorRef, persistenceProps: Props) extends Actor {
  ...
  ...
  
  val persistence = /* create persistence actor and watch it */
    context.system.actorOf(persistenceProps)

  override val supervisorStrategy = OneForOneStrategy() {
    case _ => Restart
  }
  
  ...
  
  /* Behavior for the secondary replica role. */
  var expectedSeq = 0L
  var acks = Map.empty[Long /* seq */, SnapshotJob]
  case class SnapshotJob(replicator: ActorRef, snapshot: Snapshot)
  case class Retry()

  context.system.scheduler.schedule(100 millis, 100 millis, self, Retry)

  val secondary: Receive = {
    case Get(key, id) => sender ! GetResult(key, kv.get(key), id)

    case Snapshot(key, optValue, seq) if seq > expectedSeq =>  /* ignore */

    case Snapshot(key, optValue, seq) if seq < expectedSeq =>
      sender ! SnapshotAck(key, seq)

    case Snapshot(key, optValue, seq) => { /* expectedSeq == seq */
      acks += seq -> SnapshotJob(sender, Snapshot(key, optValue, seq))
      expectedSeq += 1

      optValue match {
        case Some(value) => kv += key -> value /* insert */
        case None        => kv -= key          /* remove */
      }

      persistence ! Persist(key, optValue, seq)
    }

    case Persisted(key, seq) => if (acks.isDefinedAt(seq)) {
      val SnapshotJob(replicator, Snapshot(_, optValue, _)) = acks(seq)

      acks -= seq
      replicator ! SnapshotAck(key, seq)
    }

    case Retry => acks map { case(seq, SnapshotJob(_, Snapshot(key, optValue, _))) =>
      persistence ! Persist(key, optValue, seq)
    }

    case Terminated => /* TODO */
  }
}
```

### Step 3

Implement the replicator so that it correctly mediates between replication requests, 
snapshots and acknowledgements.

```scala
object Replicator {
  ...
  ...
  
  case class Resend()
  case class Batch()
  case class BatchJob(client: ActorRef, replicate: Replicate)
  
  ...
}
  
class Replicator(val replica: ActorRef) extends Actor {
  import Replicator._
  import Replica._
  import context.dispatcher

  // map from sequence number to pair of sender and request
  var acks = Map.empty[Long, BatchJob]
  // a sequence of not-yet-sent snapshots (you can disregard this if not implementing batching)
  var pending = Map.empty[String /* key */, BatchJob]



  var _seqCounter:Long = 0L

  def nextSeq = {
    val ret = _seqCounter
    _seqCounter += 1
    ret
  }

  val cancelTokenForResend =
    context.system.scheduler.schedule(200 millis, 200 milli, self, Resend)
  val cancelTokenForBatch =
    context.system.scheduler.schedule(100 millis, 100 millis, self, Batch)

  /* Behavior for the Replicator. */
  def receive: Receive = {
    case Replicate(key, optValue, id) =>
      if (!pending.isDefinedAt(key) || pending(key).replicate.id < id)
        pending += key -> BatchJob(sender, Replicate(key, optValue, id))

    case Batch =>{
      pending.map {
      case (key, BatchJob(primary, Replicate(_, optValue, id))) =>
        val seq = _seqCounter
        nextSeq

        val snapshot = Snapshot(key, optValue, seq)

        acks += seq -> BatchJob(primary, Replicate(key, optValue, id))
        replica ! snapshot
      }

      pending = Map.empty
    }

    case SnapshotAck(key, seq) => if (acks.isDefinedAt(seq)) {
      val BatchJob(primary, Replicate(_, _, id)) = acks(seq)

      acks -= seq

      primary ! Replicated(key, id)
    }

    case Resend => acks map { case(seq, BatchJob(_, Replicate(key, optValue, _))) =>
        replica ! Snapshot(key, optValue, seq)
    }
  }


  override def postStop() = {
    cancelTokenForBatch.cancel()
    cancelTokenForResend.cancel()
  }
}
```

### Step 2

Implement the secondary replica role so that it correctly responds to 
the read-only part of the KV protocol and accepts the replication protocol, 
without considering persistence.

```scala
  var expectedSeq = 0L

  /* Behavior for the secondary replica role. */
  val secondary: Receive = {
    case Get(key, id) =>
      sender ! GetResult(key, kv.get(key), id)

    case Snapshot(key, valueOption, seq) => valueOption match {
        case _ if seq < expectedSeq =>
          sender ! SnapshotAck(key, seq)

        case _ if seq > expectedSeq =>
          /* ignore */

        case Some(value) => /* Insert */
          kv += (key -> value)
          expectedSeq += 1
          sender ! SnapshotAck(key, seq)

        case None => /* Remove */
          kv -= key
          expectedSeq += 1
          sender ! SnapshotAck(key, seq)
      }
  }
```

### Step 1

Implement the primary replica role so that it correctly responds to 
the KV protocol messages without considering persistence or replication.

```scala
class Replica(val arbiter: ActorRef, persistenceProps: Props) extends Actor {
  import Replica._
  import Replicator._
  import Persistence._
  import context.dispatcher

  var kv = Map.empty[String, String]
  // a map from secondary replicas to replicators
  var secondaries = Map.empty[ActorRef, ActorRef]
  // the current set of replicators
  var replicators = Set.empty[ActorRef]

  /* send `Join` to the arbiter */
  arbiter ! Join

  def receive = {
    case JoinedPrimary   => context.become(leader)
    case JoinedSecondary => context.become(replica)
  }

  /* TODO Behavior for  the leader role. */
  val leader: Receive = {
    case Get(key, id) =>
      sender ! GetResult(key, kv.get(key), id)

    case Insert(key, value, id) =>
      kv += (key -> value)
      sender ! OperationAck(id)

    case Remove(key, id) =>
      kv -= key
      sender ! OperationAck(id)
  }

  /* TODO Behavior for the replica role. */
  val replica: Receive = {
    case _ =>
  }
}
```

## Requirement
 
### Lookup 
 
- A failed `Insert` or `Remove` command results in an `OperationFailed(id)` reply. 
A failed is defined as the inability to confirm the operation within 1 second.  


### Consistency guarantees

```scala
Insert("key1", "a")
Insert("key2", "1")
Insert("key1", "b")
Insert("key2", "2")
```

#### Primary Replica

A second client (not the client who issued above `Insert` operations) is not allowed to see
 
- key1 containing b and then containing a (since a was written before b for key1)
- key2 containing 2 and then containing 1 (since 1 was written before 2 for key2)

In contrast, the second client may be observe

- key1 containing b and then key2 containing 1
- key2 containing 2 and then key1 containing a

This means that the ordering guarantee only applies between reads and write to **the same** key, 
not across key

#### Secondary Replica

It must be guaranteed that a client reading from a secondary replica will eventually see 

- `key1` containing `b`
- `key2` containing `2`

#### Ordering guarantees for clients contacting different replicas

If a second client asks different replicas for the same key, it may observe different values during 
the time window when an update is disseminated. The client asking for key1 might see
                                                
- answer `b` from one replica
- and subsequently answer `a` from a different replica 

Assuming that the client keeps asking repeatedly, 
eventually all reads will result in the value `b` if no other updates are done on `key1`.

**Eventual Consistency** means that given enough time, all replicas settle on the same view. 
This also means that when you design your system clients contacting multiple replicas at the same time 
are not required to see any particular ordering.

#### Durability guarantees of updates for client contacting the primary replica

The Primary replica must reply with an `OperationAck(id)` or `OperationFailed(id)` message, 
to be sent at most **1 second** after the update command was processed

A positive `OperationAck` reply must be sent as soon as

- the change in question has been handed down to the `Persistence` module **and** 
a corresponding acknowledgement has been received from it. 

`Persistence` module fails randomly. So we should keep it alive 
while retrying *unacknowledged persistence operations* until they succeed

- replication of the change in question has been initiated **and** 
all of the secondary replicas have acknowledged the replication of the update

<br/>

If replicas leave the cluster, which is signalled by sending a new `Replicas` message to the primary, 
then outstanding acknowledgements of these replicas must be waived. This can lead to the generation of 
an `OperationAck` triggered indirectly by the `Replicas` message.

<br/>

A negative `OperationFailed` reply must be sent if the conditions for sending 
an `OperationAck` are not met within the 1 second maximum response time.

#### Consistency in the case of failed replication or persistence

Assuming in the above scenario that the last write fails (i.e. an OperationFailed is returned),  
replication to some replicas may have been successful while it failed on others. 
Therefore in this case the property that eventually all replicas converge on 
the same value for key2 is not provided by this simplified key–value store. 
In order to restore consistency a later write to key2 would have to succeed. 
Lifting this restriction is an interesting exercise on its own, 
but it is outside of the scope of this course.

One consequence of this restriction is that each replica may immediately hand out the updated 
value to subsequently reading clients, even before the the new value has been persisted locally, 
and no rollback is attempted in case of failure.

#### Which value to expect while an update is outstanding?

Sending an update request for a key followed by a `Get` request for the same key without waiting for 
the acknowledgement of the update is allowed to return either the old or the new value 
(or a third value if another client concurrently updates the same key). 

```scala
Insert("key1", "a")
<await confirmation>
Insert("key1", "b")
Get("key1")
```

The replies for the last two requests may arrive in any order, and the reply for the `Get` 
request may either contain `"a"` or `"b"`

### The Arbiter

The `Arbiter` follows a simple protocol 
 
- New replicas must first send a `Join` message to the `Arbiter` signaling that they are ready to be used.
- The `Join` message will be answered by either a `JoinedPrimary` or `JoinedSecondary` message indicating the role of the new node
- The `Arbiter` will send a `Replicas` message to the primary replica whenever it receives the `Join` message. 
This message contains the set of available replica nodes including the primary and all the secondary

### The Replicas

```scala
class Replica(val arbiter: ActorRef) extends Actor
```

The new actor stated, it must send a `Join` message to the `Arbiter` and then choose between 
primary or secondary behavior according to the reply of the `Arbiter` to the `Join` message.

#### Primary Replica

The primary replica must provide the following features

- The primary must accept update and lookup operations from clients following the Key-Value protocol 
respecting the consistency guarantee described in the above section.

- The primary must replicate changes to the secondary replicas of the system. It must also react 
to changes in membership (`Replicas` message from the `Arbiter`) and start replicating the newly joined nodes, 
and stop replication to nodes that have left. The letter implies terminating the corresponding `Replicator` actor.

#### Secondary Replica

The secondary replica must provide the following features 

- must accept the lookup operation `Get` from clients following the **Key-Value** protocol.

- The replica node must accept replication events, updating their current state.

### The Replication Protocol

When a new replica joins the system, the primary receives a new `Replica` message and must 
allocate a new actor of type `Replicator` for the new replica.

When a replica leaves the system its corresponding `Replicator` must be terminated. 

The role of this `Replicator` actor is to accept **update events**, and propagate the changes 
to its corresponding replica. In other words, There is exactly one `Replicator` per a secondary replica. 

Also, **notice** that at creation time of the `Replicator`, the primary must forward update events 
for every key-value pair it currently hold the the `Replicator`.

<br/>

```scala
class Replicator(val replica: ActorRef) extends Actor
```

The replication protocol includes two pairs of messages. The first one is used by the replica actor 
which requests replication of an update 

- `Replicate(key, valueOption, id)` is sent to the `Replicator` to initiate the replication of 
the given update to the `key`: in case of an `Insert` operation the `valueOption` will be `Some(value)` 
while in case of a `Remove` operation it will be `None`

- `Replicated(key, id)` is sent as a reply to the corresponding `Replicate` message once replication 
of that update has been successfully completed.

The second pair is used by the replicator when communicating with partner replica

- `Snapshot(key, valueOption, seq)` is sent by the `Replicator` to the appropriate secondary replica 
to indicate a new state of the given key. `valueOption` has the same meaning as for `Replicate` message

The `Snapshot` message provides a sequence number `seq` to enforce ordering between the updates. 
Updates for a given secondary replica must be processed in contiguous ascending sequence number order; 
This ensures that updates for every single key are applied in the correct order. 
Each `Replicator` uses its own number sequence starting at zero.

- When a snapshot arrives at a `Replica` with a sequence number which is greater than the currently 
expected number, then that snapshot must be ignored.

- When a snapshot arrives with smaller sequence number than currently expected number, then that 
snapshot must be ignored and immediately acknowledged as describe below.

The sender reference who sending the `Snapshot` message must be the `Replicator` actor (not the primary actor or any other)

`SnapshotAck(key, seq)` is the reply sent by the secondary replica to the `Replicator` as soon as 
the update is persisted locally by the secondary replica. 
The replica might never send this reply in case it is unable to persist the update.

The acknowledgement is sent immediately for requests whose sequence number is less than the next expected number.

The expected number is set to the greater of 

- the previously expected number
- the sequence number just acknowledged, incremented by one

Note that `Replicator` may handle multiple snapshot of a given key in parallel. For example 
Their replication has been initiated but not yet completed. It is allowed -but not required- to batch 
changes before sending them to th secondary replica, provided that each replication request is 
acknowledged properly and in the right sequence when completed. An example:

```scala
Replicate("a_key", Some("value1"), id1)
Replicate("a_key", Some("value2"), id2)
```

might have reached the `Replicator` before it got around to send a `Snapshot` message for `a_key` to its replica. 
These two messages could then result in only the following replication message

```scala
Snapshot("a_key", Some("value2"), seq)
```

skipping the state where `a_key` contains the value `value1`

Since te replication protocol is meant to symbolize remote replication you must consider the case that 
either a `Snapshot` message or its corresponding `SnapshotAck` message is lost on the way. 
Therefore the `Replicator` must make sure to periodically retransmit all unacknowledged changes. 

For grading purposes it is assumed that this happens roughly every 100 milliseconds. 
To allow for batching we will assume that a lost `Snapshot` message will lead to a resend at most 200 milliseconds 
after the `Replicate` request was received.

### Persistence

Each replica will have to submit incoming updates to the local `Persistence` actor 
and wait for its acknowledgement before confirming the update to the requester. In case of the primary, 
the requester is a client which sent an `Insert` or `Remove` request and the confirmation is an `OperationAck`, 
whereas in the case of a secondary the requester is a `Replicator` sending a `Snapshot` and expecting a `SnapshotAck` back.

The used message types are

- `Persist(key, valueOption, id)` is sent to the `Persistence` actor to request the given state to be persisted 
(with the same field description as for the `Replicate` message)

- `Persisted(key, id)` is sent by `Persistence` actor as reply in case the corresponding request was successful. No reply is sent otherwise.

Since `Persistence` is rather unreliable (failure with exception, no acknowledgement), 
it is job of the `Replica` actor to create and appropriately supervise the `Persistence` actor.

For the purpose of the exercise an strategy wil work, which means that you can experiment with different designs 
based on resuming, restarting or stopping and recreating the `Persistence` actor. 

To this end your `Replica` does not receive an `ActorRef` but a `Props` for this actor, 
implying that the `Replica` has to initially create it as well.

For grading purposes it is expected that `Persist` is retied before the **1 second** response timeout 
in case persistence failed. The `id` used in retired `Persist` message must match one which was used in the first request 
for this particular update.




