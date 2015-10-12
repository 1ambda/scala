package state

import util.TestUtils
import scalaz._, Scalaz._

class StateBasics extends TestUtils {

  type Cache = Map[String, Int]

  "create, run State" in {
    val s: State[Cache, Int] = State { c => (c, c.getOrElse("1ambda/scala", 0))}
    val c: Cache = Map("1ambda/scala" -> 1)

    val (s1, star1) = s.run(c)
    val (s2, star2) = s.run(Map.empty)

    (s1, star1) shouldBe (Map("1ambda/scala" -> 1), 1)
    (s2, star2) shouldBe (Map(), 0)
  }

  def getStargazer1(url: String): State[Cache, Int] = State { c =>
    c.get(url) match {
      case Some(count) => (c, count)
      case None        => (c.updated(url, 0), 0)
    }
  }

  def getStargazer(url: String): State[Cache, Int] =
    for {
      c <- State.get[Cache]
      optCount = c.get(url)
      _ <- modify { c: Cache =>
        optCount match {
          case Some(count) => c
          case None        => c.updated(url, 0)
        }
      }
    } yield optCount.getOrElse(0)

  "getStargazer" in {
    val c: Cache = Map("1ambda/scala" -> 1)

    val s1 = getStargazer("1ambda/haskell")
    val (c1, star) = s1.run(c)

    (c1, star) shouldBe (c.updated("1ambda/haskell", 0), 0)
  }

  Validation

}
