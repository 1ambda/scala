package transformer

import transformer.Language
import util.WordTestSuite
import scalaz._, Scalaz._
import GithubService._

class OptionTSpec extends WordTestSuite with OptionTSpecFixtures {

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

  "findLanguage2" in {
    val s = for {
      o1 <- findLanguage2(users, "1ambda", "akka", "bash")
      o2 <- findLanguage2(users, "1ambda", "akka", "java")
    } yield o1

//    println(s.runZero[List[Language]])
  }

  "findLanguage3" in {
    val s = for {
      o1 <- findLanguage3(users, "1ambda", "akka", "bash")
      o2 <- findLanguage3(users, "1ambda", "akka", "java")
    } yield o1

//    println(s.run.runZero[List[Language]])
  }

  "liftM" in {
    // type LangState[A] = State[List[Language], A]
    val l = Language("lisp", 309)
    // same as MonadTrans[OptionT].liftM(l.point[LangState])
    val os1: OptionT[LangState, Language] = l.point[LangState].liftM[OptionT]
    val os2: OptionT[LangState, Language] = OptionT(l.some.point[LangState])

    os1 === os2
    os1.run === os2.run
    os1.run.runZero[List[Language]] === os2.run.runZero[List[Language]]
  }

  "findLanguage with OptionT" in {
    val s1 = for {
      o1 <- findLanguage3(users, "1ambda", "akka", "bash")
      o2 <- findLanguage3(users, "1ambda", "akka", "java")
    } yield o1

    val s2 = for {
      o1 <- findLanguage(users, "1ambda", "akka", "bash")
      o2 <- findLanguage(users, "1ambda", "akka", "java")
    } yield o1

    s1.run === s2.run
  }

  "findLanguages" in {
    val lookups = List(
      LanguageLookup("1ambda", "akka", "scala"),
      LanguageLookup("1ambda", "akka", "erlang?!")
    )

    val (longLangs1, optFoundLangs1) = findLanguages1(users, lookups).run.runZero
    optFoundLangs1 shouldBe none

    val (longLangs2, foundOptLangs2) = findLanguages2(users, lookups).runZero
    foundOptLangs2.length shouldBe 2
    foundOptLangs2 shouldBe List(Language("scala", 4990).some, none)
  }
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
