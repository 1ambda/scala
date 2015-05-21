package lecture.crawler

import akka.actor.{Actor, Status}
import akka.pattern._
import org.jsoup.Jsoup

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContextExecutor

class Getter(url: String, depth: Int) extends Actor {
  import Getter._
  implicit val exec: ExecutionContextExecutor = context.dispatcher

  WebClient get url pipeTo self

  // same as
  // val f: Future[String] = WebClient.get(url)
  // f.onComplete {
  //   case Success(body) => self ! body
  //   case Failure(t)    => self ! Status.Failure(t)
  // }

  def receive = {
    case body: String =>
      for (link <- findLinks(body))
        context.parent ! Controller.Check(link, depth)

    case s: Status.Failure => stop()
    case Abort             => stop()
  }

  def stop() = {
    context.parent ! Done
    context.stop(self)
  }


  def findLinks(body: String) = {
    val document = Jsoup.parse(body, url)
    val links = document.select("a[href]")

    for (link <- links.iterator().asScala) yield link.absUrl("href")
  }
}

object Getter {
  sealed trait GetterEvent
  case object Done extends GetterEvent
  case object Abort extends GetterEvent
}
