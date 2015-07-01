package collections

import forkjoin.ExecutorUtils
import thread.ThreadUtils


import scala.collection._
object ConcurrentCollection {}

object CollectionsBad extends App with ExecutorUtils with ThreadUtils {
  val buffer = mutable.ArrayBuffer[Int]()

  def asyncAdd(numbers: Seq[Int]) = execute {
    buffer ++= numbers
    log(s"buffer = $buffer")
  }

  asyncAdd(0 until 10)
  asyncAdd(10 until 20)
  Thread.sleep(500)
}


