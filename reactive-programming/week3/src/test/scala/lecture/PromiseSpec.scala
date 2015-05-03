package lecture

import org.junit.runner.RunWith
import org.scalatest.concurrent.AsyncAssertions
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{ParallelTestExecution, Matchers, FunSuite}
import org.scalatest.junit.JUnitRunner

import scala.concurrent.{Promise, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Random}

@RunWith(classOf[JUnitRunner])
class PromiseSpec extends FunSuite with Matchers with AsyncAssertions with ParallelTestExecution {

  def randomSleep = Thread.sleep(Random.nextInt(500))
  def getAccountFromRemote: Future[String] = Future {
    randomSleep
    "A9-20401-7D-REMOTE"
  }

  def getAccountFromLocal: Future[String] = Future {
    randomSleep
    "A9-20401-7D-LOCAL"
  }

  def getAccount() = {
    val p = Promise[String]()

    val local = getAccountFromLocal
    val remote = getAccountFromRemote

    remote onComplete { p.tryComplete(_) }
    local  onComplete { p.tryComplete(_) }

    p.future
  }

  test("promise can be used racing 2 futures") {

    val w = new Waiter
    val limit = timeout(Span(2, Seconds))

    getAccount onComplete {
      case Success(acc) =>
        acc should (
          equal ("A9-20401-7D-REMOTE") or
          equal ("A9-20401-7D-LOCAL")
        )
        w.dismiss()
      case _ => w.dismiss()
    }

    w.await(limit)
  }
}
