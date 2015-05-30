package lecture.crawler

import akka.actor.{ActorRef, Actor}
import akka.pattern._

class Cache extends Actor {
  import Cache._
  import scala.concurrent.ExecutionContext.Implicits.global
  var cache = Map.empty[String, String]

  def receive = {
    case Get(url) =>
      if (cache contains url) sender ! cache(url)
      else {
        val client: ActorRef = sender /* sender might be different from origin in the map function*/
        WebClient get(url) map(Result(client, url, _)) pipeTo self
      }

    case Result(client, url, body) =>
      cache += url -> body
      client ! body
  }

}

object Cache {
  sealed trait CacheEvent
  case class Get(url: String) extends CacheEvent
  case class Result(client: ActorRef, url: String, body: String)
}