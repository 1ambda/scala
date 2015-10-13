package monad_transformer

import util.TestUtils
import scalaz._, Scalaz._
import GithubService._

class OptionTSpec extends TestUtils with OptionTSpecFixtures {

  "findLanguage1" in {
    val l1 = findLanguage1(users, "1ambda", "akka", "scala")
    val l2 = findLanguage1(users, "1ambda", "akka", "haskell")
    val l3 = findLanguage1(users, "1ambda", "rx-scala", "scala")
    val l4 = findLanguage1(users, "njir", "rx-scala", "scala")

    l1.isDefined shouldBe true
    l2.isDefined shouldBe false
    l3.isDefined shouldBe false
    l4.isDefined shouldBe false
  }



  OptionT
}

trait OptionTSpecFixtures {
  val u1 = User(
    "1ambda", List(
      Repository("akka", List(
        Language("scala", 4990),
        Language("java",  12801),
        Language("bash",  490)
      )),

      Repository("scalaz", List(
        Language("scala", 1451),
        Language("java",  291)
      ))
    )
  )

  val u2 = User(
    "2ambda", List()
  )

  val users = List(u1, u2)
}
