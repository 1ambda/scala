package future

import _root_.forkjoin.ExecutorUtils
import thread.ThreadUtils

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.control.NonFatal
import scala.util.{Try, Success, Failure}
import scala.io.Source

class FutureExample {}

object FutureCreate extends App with ThreadUtils with ExecutorUtils {
  Future { log("the future is here") }
  log("the future is coming")
  Thread.sleep(1000)
}

object FutureDataType extends App with ThreadUtils with ExecutorUtils {
  val buildFile: Future[String] = Future {
    val f = Source.fromFile("build.sbt")
    try f.getLines.mkString("\n") finally f.close()
  }

  log(s"started reading the bulid file async")
  log(s"statue: ${buildFile.isCompleted}")
  Thread.sleep(250)
  log(s"statue: ${buildFile.isCompleted}")
  log(s"value: ${buildFile.value}")
}

object FuturesCallbacks extends App with ThreadUtils with ExecutorUtils {

  def getUrlSpec(): Future[List[String]] = Future {
    val url = "http://www.w3.org/Addressing/URL/url-spec.txt"
    val f = Source.fromURL(url)

    try f.getLines().toList finally f.close();
  }

  def find(lines: List[String], keyword: String): String =
    lines.zipWithIndex collect {
      case (line, n) if line.contains(keyword) => (n, line)
    } mkString("\n")

  val urlSpec:Future[List[String]] = getUrlSpec()
  urlSpec foreach {
    case lines => log(find(lines, "telnet"))
  }

  log("callback registered, continuing with other work")
  Thread.sleep(2000)
}

object FutureFailure extends App with ThreadUtils with ExecutorUtils {
  val urlSpec: Future[String] = Future {
    val invalidUrl = "http://example.com/non-exists/google"

    Source.fromURL(invalidUrl).mkString
  }

  urlSpec.failed foreach {
    case t => log(s"exception occurred - $t")
  }

  Thread.sleep(1000)
}

object FuturesTry extends App with ThreadUtils with ExecutorUtils {
  val threadName: Try[String] = Try(Thread.currentThread.getName)
  val someText: Try[String] = Try("Try objects are synchronous")
  val message: Try[String] = for {
    tn <- threadName
    st <- someText
  } yield s"Message $st was create on t = $tn"

  def handleMessage(t: Try[String]) = t match {
    case Success(msg) => log(msg)
    case Failure(error) => log(s"unexpected failure - $error")
  }

  handleMessage(message)
}

object FuturesNonFatal extends App with ThreadUtils {
  val f = Future { throw new InterruptedException }
  val g = Future { throw new IllegalArgumentException }

  f.failed foreach { case t => log(s"error $t")}
  g.failed foreach { case NonFatal(t) => log(s"error $t")}
}

object FutureBlockingExample1 extends App with ThreadUtils {
  val startTime = System.nanoTime

  val futures = for (_ <- 0 until 16) yield Future {
    Thread.sleep(1000)
  }

  for (f <- futures) Await.ready(f, Duration.Inf)

  val endTime = System.nanoTime

  log(s"Total time = ${(endTime - startTime) / 1000000}")
  log(s"Total CPUs = ${Runtime.getRuntime.availableProcessors}")
}

object FutureBlockingExample2 extends App with ThreadUtils {
  val startTime = System.nanoTime

  val futures = for (_ <- 0 until 16) yield Future {
    blocking { Thread.sleep(1000) }
  }

  for (f <- futures) Await.ready(f, Duration.Inf)

  val endTime = System.nanoTime

  log(s"Total time = ${(endTime - startTime) / 1000000}")
  log(s"Total CPUs = ${Runtime.getRuntime.availableProcessors}")
}
