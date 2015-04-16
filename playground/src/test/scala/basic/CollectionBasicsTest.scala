import org.scalatest._

class CollectionBasicsTest extends FlatSpec with Matchers {
  /*
   * http://twitter.github.io/scala_school/ko/collections.html
   */

  "Set" should "not allow duplicated element" in {
    val set = Set(1, 2, 2, 3)

    assert(set.size == 3)
  }

  "Tuple" can "be parsed by pattern matching" in {

    // preparation for test
    val localNginx = ("localhost", 80)
    val localMySQL = ("localhost", 3306)
    val remoteWeb = ("awesome.service.com", 80)

    case class NotRegisteredServiceException(message: String) extends Exception(message)

    def redirect(req: (String, Int)): (String, Int) = {
      req match {
        case ("awesome.service.com", 80) => ("211.61.31.107", 80)
        case req: (String, Int) if req._1 == "localhost" => ("127.0.0.1", req._2)
        case _ => throw NotRegisteredServiceException("Request to Unknown Service")
      }
    }

    // test case

    val (host1, port1) = redirect(localNginx)
    assert((host1, port1) == ("127.0.0.1", localNginx._2))

    val (host2, port2) = redirect(remoteWeb)
    assert((host2, port2) == ("211.61.31.107", remoteWeb._2))

    val unknownService = ("great.service.com", 80)

    try {
      val (host3, port3) = redirect(unknownService)
    } catch {
      case e: NotRegisteredServiceException => ;
      case _: Throwable => fail("should catch NotRegisteredServiceException")
    }
  }

  "Map" can "contain function" in {
    def adder(x: Int, y:Int): Int = x + y
    def add2(x:Int):Int = adder(2, x:Int)
    val add3 = adder(3, _:Int)

    // Once difference type of function is saved in a map, It can't be called as a function
    // Because It's type is inferred as AnyRef
    // http://stackoverflow.com/questions/25599999/calling-functions-saved-in-a-map
    val map1 = Map(
      "adder" -> { adder(_, _) },
      "add2" -> { add2(_) },
      "add3" -> { add3(_) }
    )

    val map2 = Map(
      "adder" -> { adder(_, _) }
    )

    assert(map2("adder")(2, 3) == 5)
  }
}
