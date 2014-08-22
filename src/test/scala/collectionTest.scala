import collection.mutable.Stack
import org.scalatest._

class CollectionSpec extends FlatSpec with Matchers{

  behavior of "List()"

  it should "have size 0" in {
    assert(List().size == 0)
  }

  it should "produce NoSuchElementException when head is invoked" in {
    intercept[NoSuchElementException] {
      List().head
    }
  }

  it should "be equal to Nil" in {
    assert(List() == Nil)
  }

  ignore should "be ignored" in {
    List().head
  }

  behavior of "List(2, 3)"

  it should "return List(3) when tail is invoked" in {
    assert(List(2, 3).tail == List(3))
  }

  it should "return 2 when head is invoked" in {
    assert(List(2, 3).head == 2)
  }

  it should "return 3 when max is called" in {
    assert(List(2, 3).max == 3)
  }

  it should "be equal to `2 :: 3 :: Nil`" in {
    val list = 2 :: 3 :: Nil
    assert(List(2, 3) == list)
  }

  behavior of "List(1, 2, 3, 4, 5)"

  it should "be equal to `List(4, 5).:::(List(1, 2, 3))`" in {
    val left = List(1, 2, 3, 4, 5)
    val right = List(4, 5).:::(List(1, 2, 3))

    left should equal (right)
  }

  it should "be equal to `List(1, 2, 3).++(List(4, 5))`" in  {
    val left = List(1, 2, 3, 4, 5)
    val right = List(1, 2, 3).++(List(4, 5))

    left should equal (right)
  }
}


