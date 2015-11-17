package readerwriterstate

import monocles.HttpRequestSpec.HttpRequest
import org.scalatest._
import readerwriterstate.{POST, GET}
import scalaz._, Scalaz._

class ReaderSpec extends FunSuite with Matchers {
  /**
   *  object Reader {
   *    def apply[E, A](f: E => A): Reader[E, A] = Kleisli[Id, E, A](f)
   *  }
   *
   *  type ReaderT[F[_], E, A] = Kleisli[F, E, A]
   *  type =?>[E, A] = Kleisli[Option, E, A]
   *  type Reader[E, A] = ReaderT[Id, E, A]
   *
   *  The point of a Reader is to supply some configuration object without having to manually
   *  (or implicitly) pass i around all the functions.
   *
   * Function (-> r) is a
   *
   * - functor
   * - applicative functor
   * - monad
   */


  /**
   * ref
   *
   * https://coderwall.com/p/ye_s_w/tooling-the-reader-monad
   * http://blog.originate.com/blog/2013/10/21/reader-monad-for-dependency-injection/
   * https://newcircle.com/s/post/1108/dependency_injection_in_scala
   * http://slides.com/danielbedo/reader-monad
   * http://www.javacodegeeks.com/2015/08/easy-validation-in-scala-using-scalaz-readers-and-validationnel.html
   */
  test("Reader basic usage") {
    val f: Reader[Int, Int] = Reader { i: Int =>
      i * 3
    }

    val g = f map (_ + 2)
    val h: Reader[Int, String] = for (i <- g) yield i.toString

    h(10) shouldBe "32"
  }

  test("Reader composition") {
    val get1 = GET("http://www.google.com/search?query=scalaz&site=github")
    val post1 = POST("http://www.google.com/search", Map("query" -> "scalaz", "site" -> "github"))
    val post2 = POST("https://www.google.com/search", Map("query" -> "scalaz", "site" -> "github"))

    val toHttpsRequest = Reader { url: String => url.replaceAll("http://", "https://") }
    val sslProxy: Reader[_ >: readerwriterstate.HttpRequest, readerwriterstate.HttpRequest] = Reader { req: readerwriterstate.HttpRequest =>
      req match {
        case request if request.url.startsWith("https://") => request
        case request: POST => request.copy(url = toHttpsRequest(request.url))
        case request: GET  => request.copy(url = toHttpsRequest(request.url))
      }
    }

    toHttpsRequest.run(get1.url) shouldBe "https://www.google.com/search?query=scalaz&site=github"
    toHttpsRequest.run(post2.url) shouldBe post2.url
    sslProxy.run(get1) shouldBe GET("https://www.google.com/search?query=scalaz&site=github")
    sslProxy.run(post1) shouldBe POST("https://www.google.com/search", Map("query" -> "scalaz", "site" -> "github"))

    val uri: Reader[GET, String] = Reader { req: GET => req.url }
    val queryString: Reader[String, String] = Reader { url: String => url.split("\\?")(1) }
    val body: Reader[String, Map[String, String]] = Reader { queries: String =>
      val qs = queries.split("&").toList
      qs.foldLeft(Map.empty[String, String]) { (acc: Map[String, String], q) =>
        val kv = q.split("=")
        acc.updated(kv(0), kv(1))
      }
    }

    val queryStringToBody: Reader[GET, Map[String, String]] = uri >==> queryString >==> body

    queryStringToBody.run(get1) shouldBe Map("query" -> "scalaz", "site" -> "github")

    val convertGetToPost: Reader[_ >: readerwriterstate.HttpRequest, POST] = Reader { req : readerwriterstate.HttpRequest =>
      req match {
        case get: GET =>
          val split = get.url.split("\\?")
          val (path, query) = (split(0), split(1))
          val postBody = body.run(query)

          POST(path, postBody)

        case post: POST => post
      }
    }

    convertGetToPost(get1) shouldBe post1

    val proxiedPost: Reader[_ >: readerwriterstate.HttpRequest, POST] = sslProxy >==> convertGetToPost

    proxiedPost.run(get1) shouldBe post2
  }

  // http://www.javacodegeeks.com/2015/08/easy-validation-in-scala-using-scalaz-readers-and-validationnel.html

}
