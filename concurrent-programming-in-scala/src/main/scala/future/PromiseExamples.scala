package future

import java.util.concurrent.CancellationException
import java.util.{concurrent, TimerTask, Timer}

import thread.ThreadUtils
import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.control.NonFatal

object PromiseExamples

object PromisesCreate extends App with ThreadUtils {
  val p = Promise[String]
  val q = Promise[String]

  p.future foreach { case x => log(s"p succeeded with $x") }
  q.future.failed foreach { case t => log(s"q failed with $t")}

  Thread.sleep(1000)

  p.success("assigned")
  q.failure(new Exception("not kept"))

  Thread.sleep(1000);
}

object PromisesCustomAsync extends App with ThreadUtils {
  def myFuture[T](b: => T): Future[T] = {
    val p = Promise[T]

    global.execute(new Runnable {
      override def run(): Unit =
        try { p.success(b) } catch { case NonFatal(e) => p.failure(e) }
    })

    p.future
  }

  val f = myFuture { "naa" + "na" * 8 + " example!"}
  f foreach { case text => log(text) }
}

object FutureUtils {
  private val timer = new Timer(true)

  def timeout(t: Long): Future[Unit] = {
    val p = Promise[Unit]

    timer.schedule(new TimerTask {
      override def run(): Unit = {
        p success ()
        timer.cancel()
      }
    }, t)

    p.future
  }
}

object PromisesCancellation extends App with ThreadUtils {
  type Cancellable[T] = (Promise[Unit], Future[T])

  def cancellable[T](b: Future[Unit] => T): Cancellable[T] = {
    val cancel = Promise[Unit]

    val f = Future {
      var r: T = b(cancel.future)
      // if the promise already has been completed returns false, otherwise true
      if (!cancel.tryFailure(new Exception))
        throw new CancellationException

      r
    }

    (cancel, f)
  }
  
  val (cancel, value) = cancellable( cancle => {
    var i = 0
    
    while (i < 5) {
      if (cancle.isCompleted) throw new CancellationException
      
      Thread.sleep(500)
      log(s"$i: working")
      i += i
    }
    
    "resulting values"
  })
  
  Thread.sleep(1500)
  cancel trySuccess ()

  log("computation cancelled")

  Thread.sleep(2000)
}