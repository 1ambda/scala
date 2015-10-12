package state

import com.github.nscala_time.time.Imports._
import GithubService._

object GithubServiceWithoutState extends GithubService {

def stargazerCount(url: URL, c: Cache): (Cache, StarCount) = {
  val (c1, optCount) = checkCache(url, c)

  optCount match {
    case Some(count) => (c1, count)
    case None => retrieve(url, c1)
  }
}

def checkCache(url: URL, c: Cache): (Cache, Option[StarCount]) =
  c.get(url) match {
    case Some(Timestamped(count, time)) if !stale(time) =>
      (c.copy(hits = c.hits + 1), Some(count))
    case _ =>
      (c.copy(misses = c.misses + 1), None)
  }

def retrieve(url: URL, c: Cache): (Cache, StarCount) = {
  val count = getStarCountFromWebService(url)
  val timestamp = Timestamped(count, DateTime.now)
  (c.update(url, timestamp), count)
}
}

