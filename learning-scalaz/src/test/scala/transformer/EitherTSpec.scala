package transformer

import transformer.{Model, Transaction, QueryService}
import org.scalatest.{FunSuite, Matchers}
import scalaz._, Scalaz._

class EitherTSpec extends FunSuite with Matchers {
  import QueryService._

  test("runQuery rollback") {
    val t = new Transaction {}
    val model = new Model {}
    val result = runQuery("qqq", model).run.eval(t)
    result.isLeft shouldBe true
  }
}

