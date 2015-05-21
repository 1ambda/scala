package lecture.crawler

import akka.actor.{ReceiveTimeout, Actor, Props}

import scala.concurrent.duration._

class CrawlerApp extends Actor {

  val receptionist = context.actorOf(Props[Receptionist], "receptionist")

  receptionist ! Receptionist.Get("http://www.google.com")

  context.setReceiveTimeout(10 seconds)

  def receive = {
    case Receptionist.Result(url, links) =>
      println(links.toVector.sorted.mkString(s"Results for '$url':\n", "\n", "\n"))

    case Receptionist.Failed(url) =>
      println(s"Failed to fetch '$url'\n")

    case ReceiveTimeout =>
      context.stop(self)
  }

  override def postStop(): Unit = {
    WebClient.shutdown()
  }
}
