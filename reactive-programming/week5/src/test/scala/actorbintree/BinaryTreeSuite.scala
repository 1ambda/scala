/**
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package actorbintree

import akka.actor.{ Props, ActorRef, ActorSystem }
import org.scalatest._
import akka.testkit.{ TestProbe, ImplicitSender, TestKit }
import scala.util.Random
import scala.concurrent.duration._

class BinaryTreeSuite(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
with FunSuiteLike with Matchers with BeforeAndAfterAll with BeforeAndAfter {

  def this() = this(ActorSystem("BinaryTreeSuite"))
  var tree: ActorRef = _
  val req1 = 1
  val req2 = 2
  val req3 = 3
  val req4 = 4
  val req5 = 5

  override def afterAll: Unit = system.shutdown()
  before {
    tree = system.actorOf(Props[BinaryTreeSet])
  }

  import actorbintree.BinaryTreeSet._

  def receiveN(requester: TestProbe, ops: Seq[Operation], expectedReplies: Seq[OperationReply]): Unit =
    requester.within(5.seconds) {
      val repliesUnsorted = for (i <- 1 to ops.size) yield try {
        requester.expectMsgType[OperationReply]
      } catch {
        case ex: Throwable if ops.size > 10 => fail(s"failure to receive confirmation $i/${ops.size}", ex)
        case ex: Throwable                  => fail(s"failure to receive confirmation $i/${ops.size}\nRequests:" + ops.mkString("\n    ", "\n     ", ""), ex)
      }
      val replies = repliesUnsorted.sortBy(_.id)
      if (replies != expectedReplies) {
        val pairs = (replies zip expectedReplies).zipWithIndex filter (x => x._1._1 != x._1._2)
        fail("unexpected replies:" + pairs.map(x => s"at index ${x._2}: got ${x._1._1}, expected ${x._1._2}").mkString("\n    ", "\n    ", ""))
      }
    }

  def verify(probe: TestProbe, ops: Seq[Operation], expected: Seq[OperationReply]): Unit = {
    ops foreach { op =>
      tree ! op
    }

    receiveN(probe, ops, expected)
    // the grader also verifies that enough actors are created
  }

  test("Contains test") {
    tree ! Contains(testActor, req1, 1)
    expectMsg(ContainsResult(req1, false))

    tree ! Contains(testActor, req2, 0)
    expectMsg(ContainsResult(req2, false))
  }

  test("Insert test") {
    tree ! Insert(testActor, req1, 1)
    expectMsg(OperationFinished(req1))

    tree ! Contains(testActor, req2, 1)
    expectMsg(ContainsResult(req2, true))

    tree ! Contains(testActor, req3, 2)
    expectMsg(ContainsResult(req3, false))
  }

  test("remove test") {
    tree ! Remove(testActor, req1, 0)
    expectMsg(OperationFinished(req1))

    tree ! Insert(testActor, req2, 0)
    expectMsg(OperationFinished(req2))

    tree ! Contains(testActor, req3, 0)
    expectMsg(ContainsResult(req3, true))

    tree ! Remove(testActor, req4, 0)
    expectMsg(OperationFinished(req4))

    tree ! Contains(testActor, req5, 0)
    expectMsg(ContainsResult(req5, false))
  }

  test("proper inserts and lookups") {
    val tree = system.actorOf(Props[BinaryTreeSet])

    tree ! Contains(testActor, id = 1, 1)
    expectMsg(ContainsResult(1, false))

    tree ! Insert(testActor, id = 2, 1)
    tree ! Contains(testActor, id = 3, 1)

    expectMsg(OperationFinished(2))
    expectMsg(ContainsResult(3, true))
  }

  test("instruction example") {
    val requester = TestProbe()
    val requesterRef = requester.ref
    val ops = List(
      Insert(requesterRef, id=100, 1),
      Contains(requesterRef, id=50, 2),
      Remove(requesterRef, id=10, 1),
      Insert(requesterRef, id=20, 2),
      Contains(requesterRef, id=80, 1),
      Contains(requesterRef, id=70, 2)
      )

    val expectedReplies = List(
      OperationFinished(id=10),
      OperationFinished(id=20),
      ContainsResult(id=50, false),
      ContainsResult(id=70, true),
      ContainsResult(id=80, false),
      OperationFinished(id=100)
      )

    verify(requester, ops, expectedReplies)
  }

  test("behave identically to built-in set (includes GC)") {
    val rnd = new Random()
    def randomOperations(requester: ActorRef, count: Int): Seq[Operation] = {
      def randomElement: Int = rnd.nextInt(100)
      def randomOperation(requester: ActorRef, id: Int): Operation = rnd.nextInt(4) match {
        case 0 => Insert(requester, id, randomElement)
        case 1 => Insert(requester, id, randomElement)
        case 2 => Contains(requester, id, randomElement)
        case 3 => Remove(requester, id, randomElement)
      }

      for (seq <- 0 until count) yield randomOperation(requester, seq)
    }

    def referenceReplies(operations: Seq[Operation]): Seq[OperationReply] = {
      var referenceSet = Set.empty[Int]
      def replyFor(op: Operation): OperationReply = op match {
        case Insert(_, seq, elem) =>
          referenceSet = referenceSet + elem
          OperationFinished(seq)
        case Remove(_, seq, elem) =>
          referenceSet = referenceSet - elem
          OperationFinished(seq)
        case Contains(_, seq, elem) =>
          ContainsResult(seq, referenceSet(elem))
      }

      for (op <- operations) yield replyFor(op)
    }

    val requester = TestProbe()
    val tree = system.actorOf(Props[BinaryTreeSet])
    val count = 1000

    val ops = randomOperations(requester.ref, count)
    val expectedReplies = referenceReplies(ops)

    ops foreach { op =>
      tree ! op
      if (rnd.nextDouble() < 0.1) tree ! GC
    }
    receiveN(requester, ops, expectedReplies)
  }
}
