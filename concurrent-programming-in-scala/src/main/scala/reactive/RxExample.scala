package reactive

import org.apache.commons.io.monitor.{FileAlterationListenerAdaptor, FileAlterationListener, FileAlterationObserver, FileAlterationMonitor}
import rx.{Observer, Observable}
import rx.lang.scala.Subscription
import thread.ThreadUtils

import scala.concurrent._
import scala.io.Source

class RxExample {}

//object RxUtils {
//  def fetchQuote(): Future[String] = Future {
//    blocking {
//      val url = "http://www.iheartquotes.com/api/v1/random?" + "show_permalink=false&show_source=false”
//      Source.fromURL(url).getLines.mkString
//    }
//  }
//
//  def fetchQuoteObservable(): Observable[String] = {
//    Observable.from(fetchQuote())
//  }
//}
