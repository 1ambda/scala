package state

import com.github.nscala_time.time.Imports._
import GithubService._
import scala.util.Random

/* ref - https://speakerdeck.com/mpilquist/scalaz-state-monad */

case class Cache(hits: Int, misses: Int, map: Map[URL, Timestamped]) {
  def get(url: URL): Option[Timestamped] = map.get(url)
  def update(url: URL, timestamp: Timestamped): Cache = {
    val m = map + (url -> timestamp)
    this.copy(map = m)
  }
}

object Cache {
  def empty = Cache(0, 0, Map())
}

case class Timestamped(count: StarCount, time: DateTime)

trait GithubService {
def stale(then: DateTime): Boolean = DateTime.now > then + 5.minutes

def getStarCountFromWebService(url: URL): StarCount = {
    delay
    exampleStarCounts.getOrElse(url, 0)
  }
}

object GithubService {
  type URL = String
  type StarCount = Int
  def delay = {
    val millis = 100 + new Random().nextInt(200) /* 100 ~ 300 */
    Thread.sleep(millis)
  }
  
  val exampleStarCounts: Map[URL, StarCount] = Map(
    "1ambda/scala" -> 2,
    "1ambda/haskell" -> 1
  )
}
