import org.scalatest.{Matchers, FunSuite}

class NaturalTransformerSpec extends FunSuite with Matchers {

  test("Option to List using Natural Transformer") {
    import scalaz._
    import Scalaz._

    val toList = new (Option ~> List) {
      override def apply[T](opt: Option[T]): List[T] = opt.toList
    }

    toList(3.some) shouldBe List(3)
    toList(true.some) shouldBe List(true)
  }


}
