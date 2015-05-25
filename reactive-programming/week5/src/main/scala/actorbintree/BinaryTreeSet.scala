package actorbintree

import akka.actor._
import scala.collection.immutable.Queue


object BinaryTreeSet {

  trait Operation {
    def requester: ActorRef
    def id: Int
    def elem: Int
  }

  trait OperationReply {
    def id: Int
  }

  /** Request with identifier `id` to insert an element `elem` into the tree.
    * The actor at reference `requester` should be notified when this operation
    * is completed.
    */
  case class Insert(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request with identifier `id` to check whether an element `elem` is present
    * in the tree. The actor at reference `requester` should be notified when
    * this operation is completed.
    */
  case class Contains(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request with identifier `id` to remove the element `elem` from the tree.
    * The actor at reference `requester` should be notified when this operation
    * is completed.
    */
  case class Remove(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request to perform garbage collection*/
  case object GC

  /** Holds the answer to the Contains request with identifier `id`.
    * `result` is true if and only if the element is present in the tree.
    */
  case class ContainsResult(id: Int, result: Boolean) extends OperationReply

  /** Message to signal successful completion of an insert or remove operation. */
  case class OperationFinished(id: Int) extends OperationReply

}


class BinaryTreeSet extends Actor {
  import BinaryTreeSet._
  import BinaryTreeNode._

  def createRoot: ActorRef = context.actorOf(BinaryTreeNode.props(0, initiallyRemoved = true))
  var root = createRoot

  var pendingQueue = Queue.empty[Operation]
  def receive = normal

  val normal: Receive = {
    case GC =>
      val newRoot = createRoot
      context.become(garbageCollecting(newRoot))
      root ! CopyTo(newRoot)

    case op: Operation => root.forward(op)
  }

  def garbageCollecting(newRoot: ActorRef): Receive = {
    case CopyFinished =>
      // same as unstashAll()
      pendingQueue.foreach { newRoot ! _ }
      pendingQueue = Queue.empty
      root = newRoot
      context.unbecome()

    case op: Operation =>
      // same as stash()
      pendingQueue = pendingQueue.enqueue(op)

    case GC => /* ignore GC while garbage collection */
  }
}

object BinaryTreeNode {
  trait Position

  case object Left extends Position
  case object Right extends Position

  case class CopyTo(treeNode: ActorRef)
  case object CopyFinished

  def props(elem: Int, initiallyRemoved: Boolean) = Props(classOf[BinaryTreeNode],  elem, initiallyRemoved)
}

class BinaryTreeNode(val elem: Int, initiallyRemoved: Boolean) extends Actor {
  import BinaryTreeNode._
  import BinaryTreeSet._

  var subtrees = Map[Position, ActorRef]()
  var removed = initiallyRemoved

  // optional
  def receive = normal

  val normal: Receive =
    insert orElse
    remove orElse
    contains orElse
    copyTo orElse {
    case _ => throw new RuntimeException("unsupported operation")
  }

  def insert: Receive = {
    case Insert(client, id, e) =>
      if (e == elem && ! initiallyRemoved) {
        removed = false
        client ! OperationFinished(id)
      } else {
        val next = nextPos(e: Int)
        if (subtrees.isDefinedAt(next)) subtrees(next) ! Insert(client, id, e)
        else {
          subtrees += (next -> context.actorOf(props(e, initiallyRemoved = false)))
          client ! OperationFinished(id)
        }
      }
  }

  def remove: Receive = {
    case Remove(client, id, e) =>
      if (e == elem && !initiallyRemoved) {
        removed = true
        client ! OperationFinished(id)
      } else {
        val next = nextPos(e: Int)
        if (subtrees.isDefinedAt(next)) subtrees(next) ! Remove(client, id, e)
        else  client ! OperationFinished(id)
      }
  }

  def contains: Receive = {
    case Contains(client, id, e) =>
      if (e == elem && !initiallyRemoved) client ! ContainsResult(id, !removed)
      else {
        val next = nextPos(e: Int)
        if(subtrees.isDefinedAt(next)) subtrees(next) ! Contains(client, id, e)
        else client ! ContainsResult(id, false) /* not exists */
      }
  }

  def nextPos(e: Int) = if (e < elem) Left else Right

  def copyTo: Receive = {
    case CopyTo(newRoot) =>
      val children = subtrees.values.toSet

      if (!removed) newRoot ! Insert(self, -1 /* self insert */, elem)
      children.foreach { _ ! CopyTo(newRoot) }

      isCopyDone(children, removed)
  }

  def copying(expected: Set[ActorRef], insertConfirmed: Boolean): Receive = {
    case OperationFinished(-1) => isCopyDone(expected, true)
    case CopyFinished => isCopyDone(expected - sender, insertConfirmed)
  }

  def isCopyDone(expected: Set[ActorRef], insertConfirmed: Boolean): Unit = {
    if(expected.isEmpty && insertConfirmed) self ! PoisonPill
    else context.become(copying(expected, insertConfirmed))
  }

  override def postStop() = context.parent ! CopyFinished
}
