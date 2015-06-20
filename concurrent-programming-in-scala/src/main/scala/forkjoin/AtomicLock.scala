package forkjoin

import java.util.concurrent.atomic.AtomicBoolean

import thread.ThreadUtils

object AtomicLock extends App with ThreadUtils with ExecutorUtils {
  private val lock = new AtomicBoolean(false)

  def customSynchronized(body: => Unit): Unit = {
    while(!lock.compareAndSet(false, true)) {}
    try body finally lock.set(false)
  }

  var count = 0; for (i <- 0 until 10) execute {
    customSynchronized { count += 1 }
  }

  Thread.sleep(1000)
  log(s"Count: $count")
}
