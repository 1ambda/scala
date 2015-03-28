package option

import org.scalatest._

// ref: http://danielwestheide.com/
class OptionTest extends FlatSpec with Matchers {

  val names = List(Some("Johanna"), None, Some("Daniel"))

  "flatMap" should "return a flat list" in  {
    val expected = List("JOHANNA", "DANIEL")
    val result = names.flatMap(xs => xs.map(_.toUpperCase()))

    result should be (expected)
  }

  "orElse" should "return the latter when former is None" in {
    case class Resource(content: String)

    val resFromConfig = None
    val resFromPath = Some(Resource("JDBC Options"))
    val res = resFromConfig orElse resFromPath

    res should be (Some(Resource("JDBC Options")))
  }
}
