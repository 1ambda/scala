package kvstore

import akka.actor.FSM.->
import akka.actor.Props
import akka.actor.Actor
import akka.actor.ActorRef
import com.sun.net.httpserver.Authenticator
import com.sun.net.httpserver.Authenticator.Retry
import org.scalatest.time.Milliseconds
import scala.concurrent.duration._

object Replicator {

  /* client <-> primary <-> replicator <-> secondary <-> persistence
                        <-> replicator <-> secondary <-> persistence
                        <-> replicator <-> secondary <-> persistence
                        <-> replicator <-> secondary <-> persistence

                primary can has multiple replicators
   */

  case class Replicate(key: String, valueOption: Option[String], id: Long)
  case class Replicated(key: String, id: Long)

  case class Snapshot(key: String, valueOption: Option[String], seq: Long)
  case class SnapshotAck(key: String, seq: Long)

  def props(replica: ActorRef): Props = Props(new Replicator(replica))
}

class Replicator(val replica: ActorRef) extends Actor {
  import Replicator._
  import Replica._
  import context.dispatcher

  case class Retry()
  case class Batch()
  case class BatchJob(client: ActorRef, replicate: Replicate)

  var acks = Map.empty[Long, BatchJob] /* map from sequence number to pair of sender and request */
  var pending = Map.empty[String /* key */, BatchJob] /* a sequence of not-yet-sent snapshots */

  var _seqCounter:Long = 0L

  def nextSeq = {
    val ret = _seqCounter
    _seqCounter += 1
    ret
  }

  val cancelTokenForResend =
    context.system.scheduler.schedule(100 millis, 100 milli, context.self, Retry)
//  val cancelTokenForBatch =
//    context.system.scheduler.schedule(100 millis, 100 millis, self, Batch)

  /* Behavior for the Replicator. */
  def receive: Receive = {
//    case Replicate(key, optValue, id) =>
//      if (!pending.isDefinedAt(key) || pending(key).replicate.id < id)
//        pending += key -> BatchJob(sender, Replicate(key, optValue, id))

//    case Batch =>{
//      pending.map {
//      case (key, BatchJob(primary, Replicate(_, optValue, id))) =>
//        val seq = _seqCounter
//        nextSeq
//
//        val snapshot = Snapshot(key, optValue, seq)
//
//        acks += seq -> BatchJob(primary, Replicate(key, optValue, id))
//        replica ! snapshot
//      }
//
//      pending = Map.empty
//    }

//    case SnapshotAck(key, seq) => if (acks.isDefinedAt(seq)) {
//      val BatchJob(primary, Replicate(_, _, id)) = acks(seq)
//
//      acks -= seq
//
//      primary ! Replicated(key, id)
//    }

    case Replicate(key, optValue, id) => {
      val seq = _seqCounter
      nextSeq

      acks += seq -> BatchJob(sender, Replicate(key, optValue, id))
      val snapshot = Snapshot(key, optValue, seq)

      replica ! snapshot
    }

    case SnapshotAck(key, seq) => {
      val BatchJob(primary, Replicate(_, _, id)) = acks(seq)
      acks -= seq

      primary ! Replicated(key, id)
    }


    case Retry => acks map { case(seq, BatchJob(_, Replicate(key, optValue, _))) =>
        replica ! Snapshot(key, optValue, seq)
    }
  }


  override def postStop() = {
    // cancelTokenForBatch.cancel()
    cancelTokenForResend.cancel()
  }
}
