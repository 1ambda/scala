package readerwriterstate

import scalaz._, Scalaz._

trait ThreadState
case object Waiting    extends ThreadState
case object Running    extends ThreadState
case object Terminated extends ThreadState
case class Thread(tid: String, name: String, state: ThreadState)
case class Process(pid: String, threads: List[Thread])

object Process {
  type Logger[A] = Writer[Vector[String], A]

  def genRandomID: String = java.util.UUID.randomUUID().toString.replace("-", "")

  def createThread(name: String): Logger[Thread] = {
    val tid = genRandomID
    Thread(tid, name, Waiting).set(Vector(s"Thread [$tid] was created"))
  }

  def createEmptyProcess: Logger[Process] = {
    val pid = genRandomID
    Process(pid, Nil).set(Vector(s"Empty Process [$pid] was created"))
  }

  def createNewProcess: Logger[Process] = for {
    mainThread <- createThread("main")
    process <- createEmptyProcess
    _ <- Vector(s"Main Thread [${mainThread.tid}] was added to Process [${process.pid}").tell
  } yield process.copy(threads = mainThread.copy(state = Running) :: process.threads)
}
