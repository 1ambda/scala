package future

import thread.ThreadUtils
import scala.async.Async.{async, await}

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

object AsyncExamples

trait AsyncUtils {
  def delay(second: Int): Future[Unit] = async {
    blocking { Thread.sleep(second * 1000)}
  }

  def countdown(n: Int)(f: Int => Unit): Future[Unit] = async {
    var i = n
    while (i > 0) {
      f(i)
      await { delay(1) }
      i -= 1
    }
  }
}

object AsyncExample1 extends App with ThreadUtils with AsyncUtils {

  async {
    log("T-minus 1 second")
    await { delay(1) }
    log("done")
  }

  Thread.sleep(2000)
}

object AsyncExample2 extends App with ThreadUtils with AsyncUtils {
  countdown(10) { n => log(s"T-minus $n seconds") } foreach {
    case _ => log(s"This program is over!")
  }

  Thread.sleep(11000)
}






