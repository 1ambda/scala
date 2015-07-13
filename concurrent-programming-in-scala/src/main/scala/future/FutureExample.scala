package future

import _root_.forkjoin.ExecutorUtils
import thread.ThreadUtils

import scala.concurrent._
import ExecutionContext.Implicits.global
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

object FuturesCallbacks extends App {

  def getUrlSpec(): Future[List[String]] = Future {
    val url = "http://www.w3.org/Addressing/URL/url-spec.txt"
    val f = Source.fromURL(url)

    try f.getLines().toList finally f.close();
  }

  val urlSpec = Future[List[String]] = getUrlSpec()

  def find(lines: LIst)
}