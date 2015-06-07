package thread

class GuardedBlocks {}

import scala.collection._

object SynchronizedPool extends App with ThreadUtils {
  private type Task = () => Unit
  private val tasks = mutable.Queue[Task]()

  object Worker extends Thread {
    setDaemon(true)

    def poll() = tasks.synchronized {
      while(tasks.isEmpty) tasks.wait()
      tasks.dequeue()
    }

    override def run(): Unit = while (true) {
      val task = poll()
      task()
    }
  }

  Worker.start()

  def async(block: => Unit) = tasks.synchronized {
    tasks.enqueue(() => block)
    tasks.notify()
  }

  async { log("Hello") }
  async { log(" world!") }

  Thread.sleep(1000)
}

object SynchronizedBadPool extends App with ThreadUtils {
  private type Task = () => Unit
  private val tasks = mutable.Queue[Task]()

  val worker = new Thread {
    def poll(): Option[Task] = tasks.synchronized {
      if (tasks.nonEmpty) Some(tasks.dequeue()) else None
    }

    override def run() = while(true) poll() match {
      case Some(task) => task()
      case None =>
    }
  }

  worker.setName("Worker")
  worker.setDaemon(true)
  worker.start()

  def async(block: => Unit) = tasks.synchronized {
    tasks.enqueue(() => block)
  }

  async { log("Hello") }
  async { log(" world!") }

  Thread.sleep(10000)

  // in sbt, type
  // set fork := true
}

object SynchronizedGuardedBlocks extends App with ThreadUtils {
  val lock = new AnyRef
  var message: Option[String] = None

  val greeter = thread {
    lock.synchronized {
      while (None == message) lock.wait()
      log(message.get)
    }
  }

  lock.synchronized {
    message = Some("Hello World!")
    lock.notify()
  }

  greeter.join()
}

