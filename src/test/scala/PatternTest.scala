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

  "Exception" can "be used with pattern matching" in {
    /*
     * http://danielwestheide.com/blog/2012/12/26/
     * the-neophytes-guide-to-scala-part-6-error-handling-with-try.html
     */

    case class Customer(age: Int, money: Double)
    class Cigarettes

    case class UnderAgeException(message: String) extends Exception(message)
    case class NotEnoughMoneyException(message: String) extends Exception(message)

    def buyCigarettets(customer: Customer): Cigarettes = {
      if (customer.age < 19)
        throw UnderAgeException(s"Customer must be older than 16 but was ${customer.age}")
      else
        new Cigarettes
    }

    // create new custommer whose age is 16
    // and try to buy cigarettes. but, attemption should fail
    val customer1 = Customer(16, 4500.0)
    try {
      buyCigarettets(customer1)
    } catch {
      case e: UnderAgeException => println(e.getMessage)
    }
  }
}
