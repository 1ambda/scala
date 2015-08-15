package stm

import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}

import thread.ThreadUtils

import scala.annotation.tailrec
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.stm._

object StmExamples

object WrongAtomicVariableUsage extends App with ThreadUtils {
  val urls = new AtomicReference[List[String]](Nil)
  val clen = new AtomicInteger(0)

  def addUrl(url: String): Unit = {

    @tailrec def append(): Unit = {
      val oldUrls = urls.get
      val newUrls = url :: oldUrls

      if (!urls.compareAndSet(oldUrls, newUrls)) append()
    }

    append()
    clen.addAndGet(url.length + 1)
  }

  def getUrlArray(): Array[Char] = {
    val array = new Array[Char](clen.get)
    val urlList = urls.get

    for ((ch, i) <- urlList.map(_ + "\n").flatten.zipWithIndex) {
      array(i) = ch
    }

    array
  }

  Future {
    try { log(s"sending: ${getUrlArray().mkString}")}
    catch { case e: Exception => log(s"Houston.. $e!")}
  }

  Future {
    addUrl("http://scala-lang.org")
    addUrl("https://github.com/scala/scala")
    addUrl("http://www.scala-lang.org/api")
  }

  Thread.sleep(1000)
}

object UrlStmExample extends App with ThreadUtils {
  val urls = Ref[List[String]](Nil)
  val clen = Ref(0)

  def addUrl(url: String): Unit = atomic { implicit txn =>
    urls() = url :: urls()
    clen() = clen() + url.length + 1
  }

  def getUrlArray(): Array[Char] = atomic { implicit txn =>
    val array = new Array[Char](clen())
    for ((ch, i) <- urls().map(_ + "\n").flatten.zipWithIndex) {
      array(i) = ch
    }

    array
  }


  Future {
    addUrl("http://scala-lang.org")
    addUrl("https://github.com/scala/scala")
    addUrl("http://www.scala-lang.org/api")
  }

  Thread.sleep(25)

  Future {
    try { log(s"sending: ${getUrlArray().mkString}")}
    catch { case e: Exception => log(s"Houston.. $e!")}
  }

  Thread.sleep(1000)
}

object CompositionSideEffects extends App with ThreadUtils {
  val myValue = Ref(0)

  def inc() = atomic { implicit txn =>
    val valueAtStart = myValue()
    Txn.afterCommit { _ =>
      log(s"Incrementing ${valueAtStart}")
    }

    myValue() = myValue() + 1
  }

  Future { inc() }
  Future { inc() }

  Thread.sleep(5000)
}

case class Node(elem: Int, next: Ref[Node])

object NodeOperations extends App with ThreadUtils {
  def nodeToString(n: Node): String = atomic { implicit txn =>
    val b = new StringBuilder
    var curr = n

    while (curr != null) {
      b ++= s"${curr.elem}"
      curr = curr.next()
    }

    b.toString()
  }


  def nodeToStringWrong(n: Node): String = {
    val b = new StringBuilder

    atomic { implicit txn =>
      var curr = n
      while (curr != null) {
        b ++= s"${curr.elem}"
        curr = curr.next()
      }
    }

    b.toString()
  }
}
