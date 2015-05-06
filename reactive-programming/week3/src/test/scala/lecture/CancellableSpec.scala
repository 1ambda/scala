package lecture

import org.junit.runner.RunWith
import org.scalatest.{Matchers, FunSuite}
import org.scalatest.junit.JUnitRunner
import org.scalatest._

import scala.concurrent.{Await, Future, Promise}
import scala.util.Success
import scala.async.Async._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

@RunWith(classOf[JUnitRunner])
class CancellableSpec extends FunSuite with Matchers {

  test("cancellable should allow stopping the computation") {

    val p = Promise[String]()

    val cancellable = Cancellable.run() { status =>
      async {
        while(status.nonCancelled) {
          var a = 3
          a = a + 3
        }
        p.success("cancelled")
      }
    }

    cancellable.cancel()
    assert(Await.result(p.future, 1 second) == "cancelled")
  }

  test("cancellable should allow completion if not cancelled") {
    val p = Promise[String]()

    val cancellable = Cancellable.run() { status =>
      async {
        while(status.nonCancelled) {
          p.success("doing")
        }

        p.success("cancelled")
      }
    }

    assert(Await.result(p.future, 1 second) == "doing")
  }
}

trait CancellableStatus {
  def isCancelled: Boolean
  def nonCancelled = !isCancelled
}

trait Cancellable {
  def cancel(): Unit
  def status: CancellableStatus
}

object Cancellable {
  def apply() = new Cancellable {
    val p = Promise[Unit]()

    override def cancel: Unit = p.tryComplete(Success(()))

    val status: CancellableStatus = new CancellableStatus {
      override def isCancelled: Boolean = p.future.value != None
    }
  }

  def run()(cont: CancellableStatus => Future[Unit]): Cancellable = {
    val cancellable = Cancellable()
    cont(cancellable.status) // run continuation feeding status
    cancellable
  }
}
