import org.scalatest._

class SetTest extends FlatSpec with Matchers {
  behavior of "Set()"

  it should "have size 0" in {
    var s : Set[Int] = Set()
    assert(s.size == 0)
  }

  it should "be empty" in  {
    var s = Set()
    assert(s.isEmpty)
  }

  behavior of "Set(1, 3, 5, 5, 7)"

  it should "have size 4" in  {
    var s : Set[Int] = Set(1, 3, 5, 5, 7)
    assert(s.size == 4)
  }

  behavior of "Set(\"banana\", \"apple\", \"grape\")"

  it should "have \"apple\" as a member" in  {
    var s : Set[String] = Set("banana", "apple", "grape")
    assert(s.exists( x => x == "apple") )
  }

  "A Set" can "be filtered and return a Set" in  {
    val s : Set[Int] = Set(1, 2, 3, 4, 5, 6)
    val subSet = s.filter( x => x % 2 == 0 );
    assert(subSet.size == 3)
  }

  "A Set" can "be intersected" in {
    val s1 : Set[String] = Set("apple", "banana", "grape", "blueberry")
    val s2 : Set[String] = Set("peach", "melon", "grape")

    val r1 = s1.&(s2);
    val r2 = s1.intersect(s2);

    assert(r1 == r2)
    assert(r1.exists(x => x == "grape"))
  }

  behavior of "Set 1, 2, 3"

  it must "contains 3" in {
    var s = Set(1, 2, 3)

    assert(s.contains(3))
  }

  it should "return false when exists( _ > 3 ) is invoked" in {
    val s = Set(1, 2, 3)

    val result = s.exists { _ > 3}
    assert(result == false)
  }

}
