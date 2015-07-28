package reactive

import org.apache.commons.io.monitor.{FileAlterationListenerAdaptor, FileAlterationListener, FileAlterationObserver, FileAlterationMonitor}
import rx.lang.scala._
import thread.ThreadUtils

import scala.io.Source
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._

object RxExample

trait RxUtils {
  val url = "http://www.iheartquotes.com/api/v1/random?show_permalink=false&show_source=false"

  def fetchQuote(): Future[String] = Future {
    blocking {
      Source.fromURL(url).getLines.mkString
    }
  }

  def fetchQuoteObservable(): Observable[String] = Observable.from(fetchQuote())

  def randomQuote = Observable.create[String] { (obs: Observer[String]) =>
    obs.onNext(Source.fromURL(url).getLines.mkString)
    obs.onCompleted()
    Subscription()
  }
}

object FetchExample1 extends App with RxUtils with ThreadUtils {
  val qs = for {
    n <- Observable.interval(0.5 seconds)
    text <- fetchQuoteObservable()
  } yield s"$n) $text"

  qs.subscribe(log _)
  Thread.sleep(1500)
}

object FileSystemMonitor {
  def modified(directory: String): Observable[String] = {
    Observable.create { observer =>
      val fileMonitor = new FileAlterationMonitor(1000)
      val fileObs = new FileAlterationObserver(directory)
      val fileLis = new FileAlterationListenerAdaptor {
        override def onFileChange(file: java.io.File) {
          observer.onNext(file.getName)
        }
      }
      fileObs.addListener(fileLis)
      fileMonitor.addObserver(fileObs)
      fileMonitor.start()
      Subscription { fileMonitor.stop() }
    }
  }

  // hot-observable
//  val fileMonitor = new FileAlterationMonitor(1000)
//  fileMonitor.start()
//
//  def hotModified(directory: String): Observable[String] = {
//    val fileObs = new FileAlterationObserver(directory)
//    fileMonitor.addObserver(fileObs)
//    Observable.create { observer =>
//      val fileLis = new FileAlterationListenerAdaptor {
//        override def onFileChange(file: java.io.File) {
//          observer.onNext(file.getName)
//        }
//      }
//      fileObs.addListener(fileLis)
//      Subscription { fileObs.removeListener(fileLis) }
//    }
//  }
}

object ColdObservables {
  def randomQuote = Observable.create[String] { (obs: Observer[String]) =>
    val url = "http://www.iheartquotes.com/api/v1/random?" +
      "show_permalink=false&show_source=false"
    obs.onNext(Source.fromURL(url).getLines.mkString)
    obs.onCompleted()
    Subscription()
  }
}


