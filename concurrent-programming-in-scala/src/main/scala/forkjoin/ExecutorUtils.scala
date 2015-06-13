package forkjoin

import scala.concurrent.ExecutionContext

trait ExecutorUtils {
  def execute(body: => Unit) = ExecutionContext.global.execute(
    new Runnable {
      override def run(): Unit = body
    }
  )
}

