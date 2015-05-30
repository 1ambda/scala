package lecture.crawler

import akka.actor.{Actor, Status}
import akka.pattern._
import org.jsoup.Jsoup

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContextExecutor

class Getter(url: String, depth: Int) extends Actor {
  implicit val exec: ExecutionContextExecutor = context.dispatcher

  WebClient get url pipeTo self

  def receive = {
    case body: String =>
      for (link <- findLinks(body))
        context.parent ! Controller.Check(link, depth)

    case s: Status.Failure => context.stop(self)
  }

  def findLinks(body: String) = {
    val document = Jsoup.parse(body, url)
    val links = document.select("a[href]")

    for (link <- links.iterator().asScala) yield link.absUrl("href")
  }
}
