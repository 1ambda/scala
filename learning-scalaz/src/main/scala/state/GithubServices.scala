package state

import com.github.nscala_time.time.Imports._
import state.GithubService._
import scalaz._, Scalaz._

object GithubServices extends GithubService {
  def stargazerCount1(url: URL): State[Cache, StarCount] =
    checkCache(url) flatMap { optCount =>
      optCount match {
        case Some(count) => State { c => (c, count) }
        case None        => retrieve(url)
      }
    }

  def stargazerCount2(url: URL): State[Cache, StarCount] = for {
    optCount <- checkCache(url)
    count <- optCount match {
      case Some(count) => State[Cache, StarCount] { c => (c, count) }
      case None        => retrieve(url)
    }
  } yield count

  def stargazerCount(url: URL): State[Cache, StarCount] = for {
    optCount <- checkCache(url)
    count <- optCount
      .map(State.state[Cache, StarCount])
      .getOrElse(retrieve(url))
  } yield count

  def checkCache1(url: URL): State[Cache, Option[StarCount]] = State { c =>
    c.get(url) match {
      case Some(Timestamped(count, time)) if !stale(time) =>
        (c.copy(hits = c.hits + 1), Some(count))
      case _ =>
        (c.copy(misses = c.misses + 1), None)
    }
  }

  /**
   *  Has potential bug.
   *  Always use `State.gets` and `State.modify`.
   */
  def checkCache2(url: URL): State[Cache, Option[StarCount]] = for {
    c <- State.get[Cache]
    optCount <- State.state {
      c.get(url) collect { case Timestamped(count, time) if !stale(time) => count }
    }
    _ <- State.put(optCount ? c.copy(hits = c.hits + 1) | c.copy(misses = c.misses + 1))
  } yield optCount

  def checkCache(url: URL): State[Cache, Option[StarCount]] = for {
    optCount <- State.gets { c: Cache =>
      c.get(url) collect { case Timestamped(count, time) if !stale(time) => count }
    }
    _ <- State.modify { c: Cache =>
      optCount ? c.copy(hits = c.hits + 1) | c.copy(misses = c.misses + 1)
    }
  } yield optCount

  def retrieve1(url: URL): State[Cache, StarCount] = State { c =>
    val count = getStarCountFromWebService(url)
    val timestamp = Timestamped(count, DateTime.now)
    (c.update(url, timestamp), count)
  }

  def retrieve(url: URL): State[Cache, StarCount] = for {
    count <- State.state { getStarCountFromWebService(url) }
    timestamp = Timestamped(count, DateTime.now)
    _ <- State.modify[Cache] { _.update(url, timestamp) }
  } yield count
}
