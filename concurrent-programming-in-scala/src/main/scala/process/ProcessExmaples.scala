package process

import thread.ThreadUtils

import scala.sys.process._

object ProcessExmaples {}

object ProcessRun extends App with ThreadUtils {
  val command = "ls"
  // sync
  val exitcode = command.!

  log(s"command exited with status $exitcode")
}

object ProcessAsync extends App with ThreadUtils {
  val lsProcess = "ls -R /".run()

  Thread.sleep(1000)

  log("Timeout - killing ls!")

  lsProcess.destroy()
}

