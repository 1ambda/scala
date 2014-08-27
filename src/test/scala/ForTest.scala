import org.scalatest._

class ForTest extends FlatSpec with Matchers {
  "Array" must "yields Array" in  {
    val array = Array(1, 2, 3, 4, 5, 6)

    val result = for (elem <- array if elem >= 4) yield elem

    def matchTest(x: Any) = x match {
      case _: Array[Int] => true 
      case _ => false
    }
(matchTest(result))
  }

  "for loop" can "be nested" in {
    val result = for(a <- 1 to 3; b <- 4 to 5) yield (a, b)
    val expected = Vector(
      (1, 4), (1, 5),
      (2, 4), (2, 5),
      (3, 4), (3, 5)
    )

    assert(result == expected)
  }

  "for loop" can "use until" in {
    val result = for(a <- 1 until 7 if a % 2 == 0) yield a
    val expected = 2 to 7 by 2

    assert(result == expected) // Range will be converted implicitly
  }

  "foreach" can "be used to iterate collections" in {
    val list = List(1, 2, 3, 4, 5);

    val expected = 15;
    var result1 = 0
    var result2 = 0
    var result3 = 0
    var result4 = 0

    list.foreach { result1 += _ }
    list.foreach( result2 += _ )
    list.foreach( x => result3 += x)
    list.foreach( (x) => { result4 += x } )

    assert(result1 == expected)
    assert(result2 == expected)
    assert(result3 == expected)
    assert(result4 == expected)
  }
}
