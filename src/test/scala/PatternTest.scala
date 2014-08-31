import org.scalatest._

class PatternTest extends FlatSpec with Matchers {

  "Int 1" should "match with 'case 1' and return \"one\"" in {
    val times: Int = 1

    val result = times match {
      case 1 => "one"
      case 2 => "two"
      case _ => "Error"
    }

    val expected = "one"
    assert(result == expected)
  }

  "Int 3" should "be checked at 'case i if i == 3' and return \"three\"" in {
    val times = 3

    val result = times match {
      case x if x == 1 => "one"
      case x if x == 3 => "three"
      case _ => "Error"
    }

    val expected = "three"
    assert(result == expected)
  }

  "case statement" should "throw runtime error when default case not exists" in {
    val times = 4

    intercept[MatchError] {
      val result = times match {
        case 3 => "three"
        case 5 => "five"
      }

      fail("MacheError should be intercepted")
    }
  }

  "case" can "match with specified type" in {
    def absoluteMinusOne(o: Any): Any = {
      o match {
        case i: Int if i < 0 => i + 1
        case i: Int => i - 1
        case d: Double if d < 0.0 => d + 1.0
        case d: Double => d - 1.0
        case _ => "Unknown Type"
      }
    }

    // -2 -> -1
    assert(absoluteMinusOne(-2) == -1)
    // +3 -> +2
    assert(absoluteMinusOne(3) == 2)
    // -5.5 -> -4.5
    assert(absoluteMinusOne(-5.5) == -4.5)
    // +9.5 -> +8.5
    assert(absoluteMinusOne(9.5) == 8.5)
    // "Unknown Type"
    assert(absoluteMinusOne("return \"Unknown Type\"") == "Unknown Type")
  }
}
