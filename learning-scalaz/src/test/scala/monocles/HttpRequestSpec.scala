package monocles

import org.scalatest._
import monocle._, Monocle._, monocle.macros._

/* ref - http://www.slideshare.net/JulienTruffaut/beyond-scala-lens */
class HttpRequestSpec extends WordSpec with Matchers {
  import HttpRequestSpec._

  val r1 = HttpRequest(GET, URI("localhost", 8080, "/ping", Map("hop" -> "5")), Map.empty, "")
  val r2 = HttpRequest(POST, URI("gooogle.com", 443, "/search", Map("keyword" -> "monocle")), Map.empty, "")

  val method = GenLens[HttpRequest](_.method)
  val uri = GenLens[HttpRequest](_.uri)
  val headers = GenLens[HttpRequest](_.headers)
  val body = GenLens[HttpRequest](_.body)

  val host = GenLens[URI](_.host)
  val query = GenLens[URI](_.query)

  val get: Prism[HttpMethod, Unit] = GenPrism[HttpMethod, GET.type] composeIso GenIso.unit[GET.type]
  val post = GenPrism[HttpMethod, POST.type] composeIso GenIso.unit[POST.type]

  "get and post" in {
    (method composePrism get).isMatching(r1) shouldBe true
    (method composePrism post).isMatching(r1) shouldBe false
    (method composePrism post).getOption(r2) shouldBe Some(())
  }

  "host" in {
    (uri composeLens host).set("google.com")(r2) shouldBe
      r2.copy(uri = r2.uri.copy(host = "google.com"))
  }

  "query" in {
    val r = (uri composeLens query composeOptional index("hop") composePrism stringToInt)
      .modify(_ + 10)(r1)

    r.uri.query shouldBe Map("hop" -> "15")

    // TODO set query using at.
    // index vs at
    // filterIndex


    // update Monocle upstream
  }

}

object HttpRequestSpec {
  sealed trait HttpMethod
  case object GET   extends HttpMethod
  case object POST  extends HttpMethod

  case class URI(host: String, port: Int, path: String, query: Map[String, String])
  case class HttpRequest(method: HttpMethod, uri: URI, headers: Map[String, String], body: String)
}
