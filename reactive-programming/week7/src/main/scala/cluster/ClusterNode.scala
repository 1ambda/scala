package cluster

import akka.actor.Actor
import akka.cluster.{ClusterEvent, Cluster}

class ClusterNode extends Actor {
  val cluster = Cluster(context.system)
  
  cluster.subscribe(self, classOf[ClusterEvent.MemberRemoved])
  
  val seedAddress = cluster.selfAddress.copy(port = Some(2552))
  cluster.join(seedAddress)
  
  override def receive = {
    case ClusterEvent.MemberRemoved(m, _) =>
      if (m.address == seedAddress) context.stop(self)
  }
}
