package cluster

import akka.actor.{Props, Address, Actor}
import akka.actor.Actor.Receive
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberRemoved, MemberUp}

case class Get(url: String)
case class Failed(url: String, message: String)

class ClusterReceptionist extends Actor {

  val cluster = Cluster(context.system)

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

    case Get(url) => sender ! Failed(url, "no nodes available")
  }

  def active(addresses: Vector[Address]): Receive = {
    case Get(url) if context.children.size < addresses.size =>
      val client = sender
      val address = pick(addresses)
      context.actorOf(Props(new Customer(client, url, address)))

    case Get(url) =>
      sender ! Failed(url, "too many parallel queries")



  }

}
