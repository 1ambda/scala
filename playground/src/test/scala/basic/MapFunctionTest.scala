import org.scalatest._

class MapFunctionTest extends FlatSpec with Matchers {
  "map function" should "return the same size collection" in {

    def greaterThanTwo(x: Int) = if (x > 2) Some(x) else None

    val list = List(1, 2, 3, 4)
    // val computed = list.map((x) => greaterThanTwo(x))
    // val computed = list.map { greaterThanTwo _ }
    val computed = list.map(greaterThanTwo)

    assert(list.size == computed.size)
    assert(computed(0) == None)
    assert(computed(1) == None)
    assert(computed(2) == Some(3))
    assert(computed(3) == Some(4))
  }

  behavior of "flatMap function"

  it should "ignore None" in {
    def greaterThanOne(x: Int) = if (x > 1) Some(x) else None

    val list = List(1, 2, 3, 4, 5)
    val result = list.flatMap(greaterThanOne)
    val expected1 = List(Some(2), Some(3), Some(4), Some(5))
    val expected2 = List(2, 3, 4, 5)

    println(result == expected1)
    println(result == expected2)
  }

  it should "flatten the results into the original list" in  {

  }
}
