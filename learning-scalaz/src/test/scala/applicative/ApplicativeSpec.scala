package applicative

import org.scalatest.{Matchers, FunSuite}

class ApplicativeSpec extends FunSuite with Matchers {

  test("applicative example") {
    import scalaz._, Scalaz._

    val f: Int => Int = x => x + 5

    val someF: Option[Int => Int] = some(f)

    some(1) <*> some(f) shouldBe 6.some

    (1.some |@| 2.some) { (x, y) => x + y }    shouldBe 3.some
  }

  test("Apply") {
    class RichInteger1() { }
    case class RichInteger2() { }

    object RichInteger1 {
      def apply() = new RichInteger1()
    }

    val r1 = RichInteger1()
    val r2 = RichInteger2()
  }

  test("HttpResponse example") {
    import scalaz._, Scalaz._

    case class HttpResponse(status: Int, body: String)

    val res1 = HttpResponse(200, "invalid")
    val res2 = HttpResponse(500, "valid")
    val res3 = HttpResponse(200, "valid")

    def validateResponseCode(res: HttpResponse): Option[Int] =
      if (200 == res.status) Some(res.status) else None

    def validateResponseBody(res: HttpResponse): Option[String] =
      if ("valid" == res.body) Some(res.body) else None

    case class Validated(status: Int, body: String)

    def validateResponse(res: HttpResponse): Option[Validated] =
      (validateResponseCode(res1) |@| validateResponseBody(res1)) apply {
        (code, body) => Validated(code, body)
      }
  }
}
