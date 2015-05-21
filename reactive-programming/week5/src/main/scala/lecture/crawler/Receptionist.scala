package lecture.crawler

import akka.actor.{Actor, ActorRef, Props}

class Receptionist extends Actor {
  import Receptionist._

  var reqNo = 0
  def runNext(queue: Jobs): Receive = {
    reqNo += 1
    if (queue.isEmpty) waiting
    else {
      val controller = context.actorOf(Props[Controller], s"c$reqNo")
      controller ! Controller.Check(queue.head.url, 2)
      running(queue)
    }
  }

  def enqueueJob(queue: Jobs, job: Job): Receive = {
    if (queue.size > 3) {
      sender ! Failed(job.url)
      running(queue)
    } else running(queue :+ job)
  }

  def receive = waiting

  val waiting: Receive = {
    case Get(url) => context.become(runNext(Vector(Job(sender, url))))
  }

  def running(queue: Jobs): Receive = {
    case Controller.Result(links) =>
      val job = queue.head
      job.client ! Result(job.url, links)
      context.stop(sender)
      context.become(runNext(queue.tail))

    case Get(url) => context.become(enqueueJob(queue, Job(sender, url)))
  }
}

object Receptionist {
  case class Job(client: ActorRef, url: String)
  type Jobs = Vector[Job]

  sealed trait ReceptionistEvent
  case class Failed(url: String) extends ReceptionistEvent
  case class Get(url: String)    extends ReceptionistEvent
  case class Result(url: String, links: Set[String]) extends ReceptionistEvent
}
