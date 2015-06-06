package thread

trait ThreadUtils {
  def thread(body: => Unit): Thread = {
    val t = new Thread { override def run(): Unit = body }
    t.start(); t
  }

  def log(message: String) =
    println(s"${Thread.currentThread().getName}: $message")
}
