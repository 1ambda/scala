package thread

import scala.annotation.tailrec

object GracefulShutdown extends App with ThreadUtils {
  import scala.collection._

  private type Task = () => Unit
  private val tasks = mutable.Queue[Task]()

  object Worker extends Thread {
    var terminated = false;

    def poll(): Option[Task] = tasks.synchronized {
      while(!terminated && tasks.isEmpty) tasks.wait()
      if (!terminated) Some(tasks.dequeue()) else None
    }

    @tailrec override def run() = poll() match {
      case Some(task) => task(); run();
      case None =>
    }

    def shutdown() = tasks.synchronized {
      terminated = true
      tasks.notify()
    }
  }

  Worker.start()

  def async(block: => Unit) = tasks.synchronized {
    tasks.enqueue(() => block)
    tasks.notify()
  }

  async { log("Hello World") }
  async { log("Hello scala!") }

  Thread.sleep(1000)

  Worker.shutdown()
}
