package cluster

import akka.actor.{ActorLogging, Props, Address, Actor}
import akka.actor.Actor.Receive
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberRemoved, MemberUp}
import cluster.Receptionist.{Get, Result}

import scala.util.Random


object Receptionist {
  sealed trait ReceptionistEvent
  case class Failed(url: String, message: String) extends ReceptionistEvent
  case class Result(url: String, links: Set[String]) extends ReceptionistEvent
  case class Get(url: String) extends ReceptionistEvent
}

class Receptionist extends Actor with ActorLogging {

  val cluster = Cluster(context.system)
  val random = new Random

  cluster.subscribe(self, classOf[MemberUp])
  cluster.subscribe(self, classOf[MemberRemoved])

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
  }

  override def receive: Receive = awaitingMembers

  val awaitingMembers: Receive = {
    case current: CurrentClusterState =>
      val addresses = current.members.toVector map (_.address)
      val notMe = addresses filter (_ != cluster.selfAddress)

      if (notMe.nonEmpty) context.become(active(notMe))

    case MemberUp(member) if member.address != cluster.selfAddress =>
      context.become(active(Vector(member.address)))

    case Get(url) => sender ! Receptionist.Failed(url, "no nodes available")
  }

  def active(addresses: Vector[Address]): Receive = {
    case MemberUp(member) if member.address != cluster.selfAddress =>
      context.become(active(addresses :+ member.address))

    case MemberRemoved(member, _) =>
      val withoutRemoved = addresses filterNot(_ == member.address)
      if (withoutRemoved.isEmpty) context.become(awaitingMembers)
      else context.become(active(withoutRemoved))

    case Get(url) if context.children.size < addresses.size =>
      val client = sender
      val address = pick(addresses)
      context.actorOf(Props(new RemoteControllerDeployer(client, url, address)))

    case Get(url) =>
      sender ! Receptionist.Failed(url, "too many parallel queries")

    case Receptionist.Failed(url, message) =>
      log.debug("Failed url: {}, message: {}", url, message)

    case Receptionist.Result(url, links) =>
      log.info("Success url: {}, links: {}", url, links)
  }

  def pick(addresses: Vector[Address]): Address = {
    /* randomly pick an address from the vector */

    val index = random.nextInt(addresses.size)
    addresses(index)
  }

}
