import org.scalatest._

class CombinatorTest extends FlatSpec with Matchers {

  // http://twitter.github.io/scala_school/ko/collections.html

  "Map" can "apply passeda a function and return a list" in {
    val numbers = List(1, 2, 3, 4)

    val expected = List(2, 4, 6, 8)
    val result1 = numbers.map( _ * 2)
    val result2 = numbers.map( (x) => x * 2)

    def timesTwo(x: Int): Int = x * 2
    val result3 = numbers.map(timesTwo)

    assert(expected == result1)
    assert(expected == result2)
    assert(expected == result3)
  }

  "foreach" should "be used to make side-effect" in {
    // because it returns Unit(void)

    val numbers = List(1, 2, 3, 4, 5)
    numbers.foreach(_ * 2)
    // but numbers is immutable. we declared it as `val`
    // So numbers itself doesn't change at all
    assert(numbers == List(1, 2, 3, 4, 5))

    var sum = 0
    numbers.foreach( sum += _ )
    assert(sum == 15)
  }

  "zip" can "make tuple" in {
    val left = List(1, 2, 3, 4)
    val right = List("a", "b", "c", "d")

    val result = left.zip(right)
    val expected = List((1, "a"), (2, "b"), (3, "c"), (4, "d"))
    assert(result == expected)
  }

  "partition" can """divide list into two parts
                     using a predicate function""" in {

    val list = List(1, 2, 3, 4)
    val result = list.partition( _%2 == 0)
    val expected = (List(2, 4), List(1, 3))

    assert(result == expected)
  }

  "drop" can "select all elements except first n ones" in {
    val list = List(2, 4, 5, 5)
    val result = list.drop(2)
    val expected = List(5, 5)

    assert(result == expected)
  }

  "dropWhile" can """drop longest prefix of elements
                     that satisfy a predicate""" in {

    val list = List(2, 4, 5, 6)
    val result1 = list.dropWhile( _ % 2 == 0 )
    val expected1 = List(5, 6)
    assert(result1 == expected1)

    val result2 = list.dropWhile( _ % 2 != 0)
    val expected2 = List(2, 4, 5, 6)
    assert(result2 == expected2)
  }

  "foldLeft" can """apply binary operator to a start value and
                    all elements of this sequence going left to right""" in {

    val list = List(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    val result1 = list.foldLeft(0) { _ + _ }
    val result2 = list.foldRight(0)( (x, y) => x + y)
    val expected = 55

    assert(result1 == expected)
    assert(result2 == expected)
  }

  "flatMap" can "return a list applying a predicate into elements" in {
    val nestedList = List(List(1, 2), List(3, 4))
    val result = nestedList.flatMap( list => list.map( _ * 2) )
    val expected = List(2, 4, 6, 8)

    assert(result == expected)

    val list = List(List(1, 2), List(3, 4))
    val flattened = list.flatten
    assert(List(1, 2, 3, 4) == flattened)
  }

  "Map" can "also use filter, find and other combinators" in {
    val people = Map("Jay" -> 26, "Bone" -> 20)

    val result1 = people.find( (person: (String, Int)) =>
      person._2 < 21
    )
    val expected1 = Some("Bone" -> 20)
    assert(result1 == expected1)

    // we can use pattern matching also
    val result2 = people.find({ case (name, age) => age < 21 })
    assert(result2 == expected1)
  }

}
