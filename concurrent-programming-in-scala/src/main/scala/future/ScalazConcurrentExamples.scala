package future

import thread.ThreadUtils

import scalaz.concurrent._

object ScalazConcurrentExamples

object ScalazExample1 extends App with ThreadUtils {
  val tombola = Future {
    scala.util.Random.shuffle((0 until 10000).toVector)
  }

  tombola.runAsync { numbers =>
    log(s"And the winner is: ${numbers.head}")
  }

  tombola.runAsync { numbers =>
    log(s"... ahem, winner is: ${numbers.head}")
  }

  Thread.sleep(3000)
}
