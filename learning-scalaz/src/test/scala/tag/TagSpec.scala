package tag

import javafx.print.PrinterJob.JobStatus

import util.{WordTestSuite, FunTestSuite}

/**
 * ref
 *
 * - http://eed3si9n.com/learning-scalaz/Tagged+type.html
 * - http://www.slideshare.net/IainHull/improving-correctness-with-types
 */

class TagSpec extends WordTestSuite {

  "Creating Tagged type" in {

    import scalaz._, Scalaz._, Tag._, Tags._, syntax.tag._

    sealed trait USD
    sealed trait EUR
    def USD[A](amount: A): A @@ USD = Tag[A, USD](amount)
    def EUR[A](amount: A): A @@ EUR = Tag[A, EUR](amount)

    val oneUSD = USD(1)
    2 * oneUSD.unwrap shouldBe 2

    def convertUSDtoEUR[A](usd: A @@ USD, rate: A)
                          (implicit M: Monoid[A @@ Multiplication]): A @@ EUR =
      EUR((Multiplication(usd.unwrap) |+| Multiplication(rate)).unwrap)

    // since ===, shouldBe in scalatest only check runtime values we need =:=

    convertUSDtoEUR(USD(1), 2) =:= EUR(2)
    //      convertUSDtoEUR(USD(1), 2) === USD(2)
    // convertUSDtoEUR(USD(1), 2) =:= EUR(3) // will fail
    // convertUSDtoEUR(USD(1), 2) =:= USD(3) // compile error
  }

  "without Scalaz" in {
//    type Tagged[T] = { type Tag = T }
//    type @@[A, T] = A with Tagged[T]
  }

  "job example1" in {
    //    case class Agent(id: String, status: String, jobType: String)
    //    case class Job(id: String, maybeAgentId: Option[String], status: String, jobType: String)
  }

  "job example2" in {
    sealed abstract class AgentStatus(val value: String)
    case object Waiting    extends AgentStatus("WAITING")
    case object Processing extends AgentStatus("PROCESSING")

    sealed abstract class JobStatus(val value: String)
    case object Created   extends JobStatus("CREATED")
    case object Allocated extends JobStatus("ALLOCATED")
    case object Completed extends JobStatus("COMPLETED")

    sealed abstract class JobType(val value: String)
    case object Small extends JobType("SMALL")
    case object Large extends JobType("LARGE")
    case object Batch extends JobType("BATCH")

    import scalaz._

    case class Agent(id: String @@ Agent,
                     status: AgentStatus,
                     jobType: JobType)

    case class Job(id: String @@ Job,
                   maybeAgentId: Option[String @@ Agent],
                   status: JobStatus,
                   jobType: JobType)


    Agent(Tag[String, Agent]("03"), Waiting, Small)
    Job(Tag[String, Job]("03"), None, Created, Small)
  }

  "job example3" in {
    sealed abstract class AgentStatus(val value: String)
    case object Waiting    extends AgentStatus("WAITING")
    case object Processing extends AgentStatus("PROCESSING")

    sealed abstract class JobStatus(val value: String)
    case object Created   extends JobStatus("CREATED")
    case object Allocated extends JobStatus("ALLOCATED")
    case object Completed extends JobStatus("COMPLETED")

    sealed abstract class JobType(val value: String)
    case object Small extends JobType("SMALL")
    case object Large extends JobType("LARGE")
    case object Batch extends JobType("BATCH")

    import scalaz._, Scalaz._, Tags._, syntax.tag._

    case class Agent(id: String @@ Agent,
                     status: AgentStatus,
                     jobType: JobType)

    sealed trait Job {
      def id: String @@ Job
      def status: JobStatus
      def jobType: JobType
    }

    case class CreatedJob(id: String @@ Job,
                          jobType: JobType) extends Job {
      val status: JobStatus = Created
    }

    case class Allocated(id: scalaz.@@[String, Job], /* why we can't use just @@ */
                         agentId: scalaz.@@[String, Agent],
                         jobType: JobType
                          ) extends Job {
      val status: JobStatus = Allocated
    }

    case class Completed(id: scalaz.@@[String, Job],
                         agentId: scalaz.@@[String, Agent],
                         jobType: JobType) extends Job {
      val status: JobStatus = Completed
    }
  }
}

