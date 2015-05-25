/**
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package actorbintree

import actorbintree.BinaryTreeSet.{ContainsResult, Contains}
import actorbintree.BinaryTreeSet
import actorbintree.BinaryTreeSet.{Contains, Insert, ContainsResult}
import akka.actor.{ Props, ActorRef, ActorSystem }
import org.scalatest._
import akka.testkit.{ TestProbe, ImplicitSender, TestKit }
import scala.util.Random
import scala.concurrent.duration._

class BinaryTreeSuite(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
with FunSuiteLike with Matchers with BeforeAndAfterAll with BeforeAndAfter {

  def this() = this(ActorSystem("BinaryTreeSuite"))
  var tree: ActorRef = _

  var requestId = 0
  def genRequestId: Int = {
    requestId += 1
    requestId
  }

  def verifyContainsFalse(tree: ActorRef, sender: ActorRef, elem: Int): Unit =
    verifyContains(tree, sender, elem, false)

  def verifyContainsTrue(tree: ActorRef, sender: ActorRef, elem: Int): Unit =
    verifyContains(tree, sender, elem, true)

  def verifyContainsFalse(tree: ActorRef, sender: ActorRef, elems: List[Int]): Unit =
    verifyContains(tree, sender, elems, false)

  def verifyContainsTrue(tree: ActorRef, sender: ActorRef, elems: List[Int]): Unit =
    verifyContains(tree, sender, elems, true)

  def verifyContains(tree: ActorRef, sender: ActorRef, elem: Int, exist: Boolean): Unit =
    verifyContains(tree, sender, List(elem), exist)

  def verifyContains(tree: ActorRef, sender: ActorRef, elems: List[Int], exist: Boolean): Unit = {
    (0 until elems.length) foreach { index =>
      var reqId = genRequestId
      tree ! Contains(sender, reqId, elems(index))
      expectMsg(ContainsResult(reqId, exist))
    }
  }

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
    verifyContainsFalse(tree, testActor, 1)
    verifyContainsFalse(tree, testActor, 0)
  }

  test("insert test") {
    verifyContainsFalse(tree, testActor, 0)

    var reqId = genRequestId
    tree ! Insert(testActor, reqId, 0)
    expectMsg(OperationFinished(reqId))

    verifyContainsTrue(tree, testActor, 0)
    verifyContainsFalse(tree, testActor, 1)
  }

  test("insert duplicated element test") {
    var reqId = genRequestId
    tree ! Insert(testActor, reqId, 0)
    expectMsg(OperationFinished(reqId))

    reqId = genRequestId
    tree ! Insert(testActor, reqId, 0)
    expectMsg(OperationFinished(reqId))

    verifyContainsTrue(tree, testActor, 0)

    reqId = genRequestId
    tree ! Insert(testActor, reqId, 1)
    expectMsg(OperationFinished(reqId))

    verifyContainsTrue(tree, testActor, 1)

    reqId = genRequestId
    tree ! Insert(testActor, reqId, 1)
    expectMsg(OperationFinished(reqId))

    verifyContainsTrue(tree, testActor, 1)
  }

  test("remove root test") {
    var reqId = genRequestId
    tree ! Remove(testActor, reqId, 0)
    expectMsg(OperationFinished(reqId))

    verifyContainsFalse(tree, testActor, 0)
  }

  test("remove single node test 1") {
    var reqId = genRequestId
    tree ! Insert(testActor, reqId, 0)
    expectMsg(OperationFinished(reqId))

    verifyContainsTrue(tree, testActor, 0)

    reqId = genRequestId
    tree ! Remove(testActor, reqId, 0)
    expectMsg(OperationFinished(reqId))

    verifyContainsFalse(tree, testActor, 0)
  }

  test("remove single node test 2") {
    var reqId = genRequestId
    tree ! Insert(testActor, reqId, 1)
    expectMsg(OperationFinished(reqId))

    reqId = genRequestId
    tree ! Remove(testActor, reqId, 1)
    expectMsg(OperationFinished(reqId))

    verifyContainsFalse(tree, testActor, 1)
  }

  test("proper inserts and lookups") {
    var reqId1 = genRequestId
    tree ! Contains(testActor, id = reqId1, 1)
    expectMsg(ContainsResult(reqId1, false))

    var reqId2 = genRequestId
    tree ! Insert(testActor, id = reqId2, 1)

    var reqId3 = genRequestId
    tree ! Contains(testActor, id = reqId3, 1)

    expectMsg(OperationFinished(reqId2))
    expectMsg(ContainsResult(reqId3, true))
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

