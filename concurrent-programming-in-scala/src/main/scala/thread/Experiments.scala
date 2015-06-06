package thread

class Experiments { }

object ThreadSleep extends App with ThreadUtils {

  val t = thread {
    Thread.sleep(1000)
    log("New thread running")
    Thread.sleep(1000)
    log("Still running")
    Thread.sleep(1000)
    log("Completed")
  }

  t.join()
  log("New thread joined")
}

object ThreadMain extends App {
  val t: Thread = Thread.currentThread()
  println(s"Thread: ${t.getName}") // `run-main-0`
}

object ThreadCreation extends App {
  class MyThread extends Thread {
    override def run(): Unit = {
      println("MyThread is running")
    }
  }

  val t = new MyThread
  t.start()
  t.join() /* wait until `t` terminate */
  println("MyThread joined")
}


