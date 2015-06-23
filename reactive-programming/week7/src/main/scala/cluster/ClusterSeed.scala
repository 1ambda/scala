package cluster

import akka.actor.{ActorLogging, Actor}
import akka.actor.Actor.Receive
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.{ClusterEvent, Cluster}

class ClusterSeed extends Actor with ActorLogging {
  val cluster = Cluster(context.system)

  cluster.subscribe(self, classOf[ClusterEvent.MemberUp])
  cluster.join(cluster.selfAddress)

  override def receive: Receive = {
    case MemberUp(member) =>
      if (member.address != cluster.selfAddress) {
        // someone joined
        log.info("member: {}, address: {}", member, member.address)
      }

    case Receptionist.Failed(url, message) =>
      log.error("url: {}, message: {}", url, message)
  }
}
