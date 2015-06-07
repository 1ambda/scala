package kvstore

import akka.actor.FSM.->
import akka.actor.{ OneForOneStrategy, Props, ActorRef, Actor }
import kvstore.Arbiter._
import scala.collection.immutable.Queue
import akka.actor.SupervisorStrategy.Restart
import scala.annotation.tailrec
import akka.pattern.{ ask, pipe }
import akka.actor.Terminated
import scala.concurrent.duration._
import akka.actor.PoisonPill
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy
import akka.util.Timeout

object Replica {
  sealed trait Operation {
    def key: String
    def id: Long
  }

  /* for primary replica */
  case class Insert(key: String, value: String, id: Long) extends Operation
  case class Remove(key: String, id: Long) extends Operation

  sealed trait OperationReply
  case class OperationAck(id: Long) extends OperationReply
  case class OperationFailed(id: Long) extends OperationReply

  /* for secondary replica */
  case class Get(key: String, id: Long) extends Operation
  case class GetResult(key: String, valueOption: Option[String], id: Long) extends OperationReply

  def props(arbiter: ActorRef, persistenceProps: Props): Props = Props(new Replica(arbiter, persistenceProps))
}

class Replica(val arbiter: ActorRef, persistenceProps: Props) extends Actor {
  import Replica._
  import Replicator._
  import Persistence._
  import context.dispatcher

  var kv = Map.empty[String, String]
  var secondaries = Map.empty[ActorRef, ActorRef] /* a map from secondary replicas to replicators */
  var replicators = Set.empty[ActorRef] /* the current set of replicators */

  arbiter ! Join /* send `Join` to the arbiter */

  val persistence = /* create persistence actor and watch it */
    context.system.actorOf(persistenceProps)

  override val supervisorStrategy = OneForOneStrategy() {
    case _ =>
      println("Persistence Actor will be restarted")
      Restart
  }

  // TODO: watch, apply strategy

  def receive = {
    case JoinedPrimary   => context.become(primary)
    case JoinedSecondary => context.become(secondary)
  }

  /* Behavior for the primary replica role. */

  val primary: Receive = {
    case Get(key, id) =>
      sender ! GetResult(key, kv.get(key), id)

    case Insert(key, value, id) =>
      kv += (key -> value)
      sender ! OperationAck(id)

    case Remove(key, id) =>
      kv -= key
      sender ! OperationAck(id)
  }

  override def postStop() = {
    cancelTokenForRetry.cancel()
  }

  /* Behavior for the secondary replica role. */
  var expectedSeq = 0L
  var secondaryAcks = Map.empty[Long /* seq */, SnapshotJob]
  case class SnapshotJob(replicator: ActorRef, snapshot: Snapshot)
  case class Retry()

  val cancelTokenForRetry =
    context.system.scheduler.schedule(100 millis, 100 millis, self, Retry)

  val secondary: Receive = {
    case Get(key, id) => sender ! GetResult(key, kv.get(key), id)

    case Snapshot(key, optValue, seq) if seq > expectedSeq =>  /* ignore */

    case Snapshot(key, optValue, seq) if seq < expectedSeq =>
      sender ! SnapshotAck(key, seq)

    case Snapshot(key, optValue, seq) => { /* expectedSeq == seq */
      secondaryAcks += seq -> SnapshotJob(sender, Snapshot(key, optValue, seq))
      expectedSeq += 1

      optValue match {
        case Some(value) => kv += key -> value /* insert */
        case None        => kv -= key          /* remove */
      }

      persistence ! Persist(key, optValue, seq)
    }

    case Persisted(key, seq) => if (secondaryAcks.isDefinedAt(seq)) {
      val SnapshotJob(replicator, Snapshot(_, optValue, _)) = secondaryAcks(seq)

      secondaryAcks -= seq
      replicator ! SnapshotAck(key, seq)
    }

    case Retry => secondaryAcks map { case(seq, SnapshotJob(_, Snapshot(key, optValue, _))) =>
      persistence ! Persist(key, optValue, seq)
    }
  }
}

