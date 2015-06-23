package cluster

import akka.actor._
import akka.pattern._
import cluster.Controller.Check
import org.jsoup.Jsoup

import scala.collection.JavaConverters._
import scala.concurrent.Future


class Getter(url: String, depth: Int) extends Actor {
  implicit val ece = context.dispatcher

  WebScrapper get url pipeTo self

  override def receive: Receive = {
    case body: String =>
      for (link <- findLinks(body)) context.parent ! Check(link, depth)


    case s: Status.Failure => /* future failed */
      context.stop(self)
  }

  def findLinks(body: String): Iterator[String] = {
    val document = Jsoup.parse(body, url)
    val links = document.select("a[href]")

    /* need scala.collection.JavaConverters._ to use asScala */
    for(link <- links.iterator().asScala) yield link.absUrl("href")
  }
}


