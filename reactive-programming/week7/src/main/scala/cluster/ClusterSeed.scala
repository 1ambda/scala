package cluster

import akka.actor.Actor
import akka.actor.Actor.Receive
import akka.cluster.{ClusterEvent, Cluster}

class ClusterSeed extends Actor {
  val cluster = Cluster(context.system)

  cluster.subscribe(self, classOf[ClusterEvent.MemberUp])
  cluster.join(cluster.selfAddress)

  override def receive: Receive = ???
}
