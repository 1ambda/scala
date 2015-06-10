package kvstore

import akka.testkit.TestKit
import akka.testkit.ImplicitSender
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers
import org.scalatest.FunSuiteLike
import akka.actor.{Props, Actor, ActorLogging, ActorSystem}
import kvstore.Arbiter.{JoinedPrimary, Replicas, JoinedSecondary, Join}
import akka.testkit.TestProbe
import Arbiter._
import Replicator._
import org.scalactic.ConversionCheckedTripleEquals
import akka.testkit.{ TestProbe, TestKit, ImplicitSender }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers
import org.scalatest.FunSuiteLike
import akka.actor.{Props, Actor, ActorLogging, ActorSystem}
import scala.concurrent.duration._
import kvstore.Arbiter.{JoinedPrimary, Replicas, JoinedSecondary, Join}
import kvstore.Persistence.{ Persisted, Persist }
import kvstore.Replicator.{ SnapshotAck, Snapshot, Replicate }
import org.scalactic.ConversionCheckedTripleEquals

class Step6_NewSecondarySpec extends TestKit(ActorSystem("Step6NewSecondarySpec"))
  with FunSuiteLike
  with BeforeAndAfterAll
  with Matchers
  with ConversionCheckedTripleEquals
  with ImplicitSender
  with Tools {

  override def afterAll(): Unit = {
    system.shutdown()
  }

  test("case1: Primary must start replication to new replicas") {
    val arbiter = TestProbe()
    val primary = system.actorOf(Replica.props(arbiter.ref, Persistence.props(flaky = false)), "case1-primary")
    val user = session(primary)
    val secondary = TestProbe()

    arbiter.expectMsg(Join)
    arbiter.send(primary, JoinedPrimary)

    user.setAcked("k1", "v1")
    arbiter.send(primary, Replicas(Set(primary, secondary.ref)))

    secondary.expectMsg(Snapshot("k1", Some("v1"), 0L))
    secondary.reply(SnapshotAck("k1", 0L))

    val ack1 = user.set("k1", "v2")
    secondary.expectMsg(Snapshot("k1", Some("v2"), 1L))
    secondary.reply(SnapshotAck("k1", 1L))
    user.waitAck(ack1)

    val ack2 = user.remove("k1")
    secondary.expectMsg(Snapshot("k1", None, 2L))
    secondary.reply(SnapshotAck("k1", 2L))
    user.waitAck(ack2)
  }

  test("case2: Primary must stop replication to removed replicas and stop Replicator") {
    val arbiter = TestProbe()
    val primary = system.actorOf(Replica.props(arbiter.ref, Persistence.props(flaky = false)), "case2-primary")
    val user = session(primary)
    val secondary = TestProbe()

    arbiter.expectMsg(Join)
    arbiter.send(primary, JoinedPrimary)
    arbiter.send(primary, Replicas(Set(primary, secondary.ref)))

    val ack1 = user.set("k1", "v1")
    secondary.expectMsg(Snapshot("k1", Some("v1"), 0L))
    val replicator = secondary.lastSender
    secondary.reply(SnapshotAck("k1", 0L))
    user.waitAck(ack1)

    watch(replicator)
    arbiter.send(primary, Replicas(Set(primary)))
    expectTerminated(replicator)
  }

  test("case3: Primary must stop replication to removed replicas and waive their outstanding acknowledgements") {
    val arbiter = TestProbe()
    val primary = system.actorOf(Replica.props(arbiter.ref, Persistence.props(flaky = false)), "case3-primary")
    val user = session(primary)
    val secondary = TestProbe()

    arbiter.expectMsg(Join)
    arbiter.send(primary, JoinedPrimary)
    arbiter.send(primary, Replicas(Set(primary, secondary.ref)))

    val ack1 = user.set("k1", "v1")
    secondary.expectMsg(Snapshot("k1", Some("v1"), 0L))
    secondary.reply(SnapshotAck("k1", 0L))
    user.waitAck(ack1)

    val ack2 = user.set("k1", "v2")
    secondary.expectMsg(Snapshot("k1", Some("v2"), 1L))
    arbiter.send(primary, Replicas(Set(primary)))
    user.waitAck(ack2)
  }

  test("case3: Primary and secondaries must work in concert when persistence and communication to secondaries is unreliable") {

    val arbiter = TestProbe()

    val (primary, user) = createPrimary(arbiter, "case3-primary", flakyPersistence = true)

    user.setAcked("k1", "v1")

    val (secondary1, replica1) = createSecondary(arbiter, "case3-secondary1", flakyForwarder = true, flakyPersistence = true)
    val (secondary2, replica2) = createSecondary(arbiter, "case3-secondary2", flakyForwarder = true, flakyPersistence = true)

    arbiter.send(primary, Replicas(Set(primary, secondary1, secondary2)))

    val options = Set(None, Some("v1"))
    options should contain (replica1.get("k1"))
    options should contain (replica2.get("k1"))

    user.setAcked("k1", "v2")
    assert(replica1.get("k1") === Some("v2"))
    assert(replica2.get("k1") === Some("v2"))

    val (secondary3, replica3) = createSecondary(arbiter, "case3-secondary3", flakyForwarder = true, flakyPersistence = true)

    arbiter.send(primary, Replicas(Set(primary, secondary1, secondary3)))

    replica3.nothingHappens(500 milliseconds)

    assert(replica3.get("k1") === Some("v2"))

    user.removeAcked("k1")
    assert(replica1.get("k1") === None)
    assert(replica2.get("k1") === Some("v2"))
    assert(replica3.get("k1") === None)

    user.setAcked("k1", "v4")
    assert(replica1.get("k1") === Some("v4"))
    assert(replica2.get("k1") === Some("v2"))
    assert(replica3.get("k1") === Some("v4"))

    user.setAcked("k2", "v1")
    user.setAcked("k3", "v1")

    user.setAcked("k1", "v5")
    user.removeAcked("k1")
    user.setAcked("k1", "v7")
    user.removeAcked("k1")
    user.setAcked("k1", "v9")
    assert(replica1.get("k1") === Some("v9"))
    assert(replica2.get("k1") === Some("v2"))
    assert(replica3.get("k1") === Some("v9"))

    assert(replica1.get("k2") === Some("v1"))
    assert(replica2.get("k2") === None)
    assert(replica3.get("k2") === Some("v1"))

    assert(replica1.get("k3") === Some("v1"))
    assert(replica2.get("k3") === None)
    assert(replica3.get("k3") === Some("v1"))
  }

  def createPrimary(arbiter: TestProbe, name: String, flakyPersistence: Boolean) = {

    val primary = system.actorOf(Replica.props(arbiter.ref, Persistence.props(flakyPersistence)), name)

    arbiter.expectMsg(Join)
    arbiter.send(primary, JoinedPrimary)

    (primary, session(primary))
  }

  def createSecondary(arbiter: TestProbe, name: String, flakyForwarder: Boolean, flakyPersistence: Boolean) = {

    val secondary =
      if (flakyForwarder) system.actorOf(Props(new FlakyForwarder(Replica.props(arbiter.ref, Persistence.props(flakyPersistence)), name)), s"flaky-$name")
      else system.actorOf(Replica.props(arbiter.ref, Persistence.props(flakyPersistence)), name)

    arbiter.expectMsg(Join)
    arbiter.send(secondary, JoinedSecondary)

    (secondary, session(secondary))
  }

}

class FlakyForwarder(targetProps: Props, name: String) extends Actor with ActorLogging {

  val child = context.actorOf(targetProps, name)

  var flipFlop = true

  def receive = {

    case msg if sender == child =>
      context.parent forward msg

    case msg:Snapshot =>
      if (flipFlop) child forward msg
      else log.debug(s"Dropping $msg")
      flipFlop = !flipFlop

    case msg =>
      child forward msg
  }
}
