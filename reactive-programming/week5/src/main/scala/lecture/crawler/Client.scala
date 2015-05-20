package lecture.crawler

import java.util.concurrent.Executor

import com.ning.http.client.AsyncHttpClient

import scala.concurrent.{Promise, Future}

class Client {
  import Client._

  val client = new AsyncHttpClient
  def getBlocking(url: String): String = {

    /* will be blokcing */
    val res = client.prepareGet(url).execute().get

    if (res.getStatusCode < 400)
      res.getResponseBodyExcerpt(131072 /* 128KB */)
    else throw BadStatus(res.getStatusCode)
  }

  def get(url: String)(implicit exec: Executor): Future[String] = {

    // java future
    val f = client.prepareGet(url).execute()
    // scala promise
    val p = Promise[String]()

    f.addListener(new Runnable {
      override def run(): Unit = {
        val res = f.get

        if (res.getStatusCode < 400) p.success(res.getResponseBodyExcerpt(131072))
        else p.failure(BadStatus(res.getStatusCode))
      }
    }, exec)

    p.future
  }
}

object Client {
  case class BadStatus(statusCode: Int) extends RuntimeException
}
