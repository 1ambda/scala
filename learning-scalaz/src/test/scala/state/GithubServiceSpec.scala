package state

import util.TestUtils
import scalaz._
import com.github.nscala_time.time.Imports._

/* ref - https://speakerdeck.com/mpilquist/scalaz-state-monad */
class GithubServiceSpec extends TestUtils {
  import GithubService._
  import Scalaz._

  val c_0 = Cache.empty
  val c_1 = Cache(0, 0, Map("1ambda/haskell" -> Timestamped(1, DateTime.now)))

  "GithubService1" in {
    val (c1, count) = GithubServiceWithoutState.stargazerCount("1ambda/haskell", c_0)

    count shouldBe 1
    (c1.hits, c1.misses) shouldBe (0, 1)
  }

  "GithubService2" in {
    val (c1, count) = GithubServices.stargazerCount("1ambda/haskell").run(c_0)

    count shouldBe 1
    (c1.hits, c1.misses) shouldBe (0, 1)
  }
  
  "State.eval returns value only" in {
    val count = GithubServices.stargazerCount("1ambda/haskell").eval(c_0)
    count shouldBe 1
  }
  
  "State.exec returns state only" in {
    val c1 = GithubServices.stargazerCount("1ambda/haskell").exec(c_1)
    (c1.hits, c1.misses) shouldBe (1, 0)
  }

  "State[S, A].runZero, evalZero, execZero requires implicit Monoid[S]" in {
    implicit val cacheMonoid: Monoid[Cache] = new Monoid[Cache] {
      override def zero: Cache = Cache(0, 0, Map())
      override def append(f1: Cache, f2: => Cache): Cache =
        Cache(f1.hits + f2.hits, f1.misses + f2.misses, f1.map ++ f2.map /* not commutative */)
    }

    val (c1, count1) = GithubServices.stargazerCount("1ambda/haskell").runZero
    count1 shouldBe 1
    (c1.hits, c1.misses) shouldBe (0, 1)

    val count2 = GithubServices.stargazerCount("1ambda/haskell").evalZero
    count2 shouldBe 1

    val c2 = GithubServices.stargazerCount("1ambda/haskell").execZero
    (c2.hits, c2.misses) shouldBe (0, 1)
  }

  "State[S, A] is Functor[S], Applicative[S] and Monad[S]" in {
    /* see: http://tpolecat.github.io/assets/scalaz.svg */
    type StateCache[A] = State[Cache, A]

    val ten = 10.point[StateCache]
    val five = ten map (_ - 5)
    five === 5.point[StateCache]
  }

  "StateCache[A].sequence" in {
    type StateCache[A] = State[Cache, A]

    val countStates: List[StateCache[StarCount]] = List(
      GithubServices.stargazerCount("1ambda/scala"),
      GithubServices.stargazerCount("1ambda/scala"),
      GithubServices.stargazerCount("1ambda/haskell")
    )

    val stateCounts: StateCache[List[StarCount]] =
      countStates.sequence[StateCache, StarCount]

    val (c, count) = stateCounts.run(Cache.empty)
    (c.hits, c.misses) shouldBe (1, 2)
  }

  "State[S, A].sequence using Type Lambda" in {
    val countStates: List[State[Cache, StarCount]] = List(
      GithubServices.stargazerCount("1ambda/scala"),
      GithubServices.stargazerCount("1ambda/scala"),
      GithubServices.stargazerCount("1ambda/haskell")
    )

    val stateCounts: State[Cache, List[StarCount]] =
      countStates.sequence[({type λ[α] = State[Cache, α]})#λ, StarCount]

    val (c, count) = stateCounts.run(Cache.empty)
    (c.hits, c.misses) shouldBe (1, 2)
  }

  "State Transformation" in {
    /**
     * Problem:
     *
     * given `State[T, ?]` how we can treat it as State[S, ?]
     */
  }

}


