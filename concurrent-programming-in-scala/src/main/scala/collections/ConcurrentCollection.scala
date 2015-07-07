package collections

import java.util.concurrent.LinkedBlockingQueue

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

object CollectionIterators extends App with ThreadUtils with ExecutorUtils {
  val queue = new LinkedBlockingQueue[String]

  for (i <- 1 to 5500) queue.offer(i.toString)

  execute {
    val it = queue.iterator
    while (it.hasNext) log(it.next())
  }

  for (i <- 1 to 5500) queue.poll()
  Thread.sleep(1000)
}

