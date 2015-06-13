package forkjoin

import thread.ThreadUtils

import scala.concurrent._
import scala.concurrent.forkjoin.ForkJoinPool

object ExecutorCreate extends App with ThreadUtils {
  val executor = new ForkJoinPool

  val a = new ExecutionContext {override def reportFailure(cause: Throwable): Unit = ???

    override def execute(runnable: Runnable): Unit = ???
  }

  executor.execute(new Runnable {
    override def run(): Unit = log("task is run async")
  })

  Thread.sleep(500);
}

object ExecutionContextCreate extends App with ThreadUtils {
  val pool = new ForkJoinPool(2)
  val ectx = ExecutionContext.fromExecutorService(pool)

  ectx.execute(new Runnable {
    override def run(): Unit = log("task is run async")
  })

  Thread.sleep(500);
}

object ExecutionContextSleep extends App with ExecutorUtils with ThreadUtils{
  for(i <- 0 until 32) execute {
    Thread.sleep(2000)
    log(s"Task $i completed.")
  }

  Thread.sleep(10000)
}
