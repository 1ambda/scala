package cluster

import java.util.concurrent.Executor

import com.ning.http.client.AsyncHttpClient

import scala.concurrent.{Promise, Future}

object WebScrapper {
  case class BadStatus(statusCode: Int) extends RuntimeException

  val client = new AsyncHttpClient

  def get(url: String)(implicit ec: Executor): Future[String] = {
    val f = client.prepareGet(url).execute() /* java future */
    val p = Promise[String]() /* scala promise */

    /* convert Java Future to Scala Future */
    f.addListener(new Runnable {
      override def run(): Unit = {
        val res = f.get

        if (res.getStatusCode < 400) p.success(res.getResponseBodyExcerpt(131072 /* 128KB */))
        else p.failure(BadStatus(res.getStatusCode))
      }
    }, ec)

    p.future
  }

  def shutdown(): Unit = client.close()
}
