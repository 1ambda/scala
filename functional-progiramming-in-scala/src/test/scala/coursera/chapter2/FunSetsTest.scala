package coursera.chapter2

import org.scalatest._

class FunSetsTest extends FlatSpec with Matchers {

  import FunSets._

  val s1 = singletonSet(1)
  val s2 = singletonSet(2)
  val s3 = singletonSet(3)
  
  "contains" should "be implemented" in {
    assert(contains(x => true, 100) == true)
  }

  "s1" should "contain 1" in {
    assert(contains(s1, 1) == true)
  }

  "singletonSet(1)" should "contains 1" in {
    assert(contains(s1, 1) == true)
  }

  "union" should "contains all elements" in {
    val s = union(s1, s2)
    assert(contains(s, 1), "Union 1")
    assert(contains(s, 2), "Union 2")
    assert(!contains(s, 3), "Union 3")
  }

  "intersect" should "contains all common elements" in {
    val s12 = union(s1, s2)
    val s13 = union(s1, s3)
    val s1Only = intersect(s12, s13)

    assert(contains(s12, 1));
    assert(contains(s12, 2));
    assert(contains(s1Only, 1));
    assert(!contains(s1Only, 2));
  }

  "diff(s, t)" should "return true when given a element is contained in s only" in {
    val s12 = union(s1, s2)
    val s13 = union(s1, s3)
    val s123 = union(s12, s3)
    val s12ButNot3 = diff(s12, s13)
    val s13ButNot2 = diff(s13, s12)
    val s123ButNot12 = diff(s123, s12)

    assert(s12ButNot3(2))
    assert(!(s12ButNot3(3)))
    assert(!(s12ButNot3(1)))
    assert(s123ButNot12(3))
    assert(!(s123ButNot12(1)))
  }

  "filter(s12345, (x: Int => x <= 3))" can "be select set" in {
    val s12 = union(s1, s2)
    val s123 = union(s12, singletonSet(3))
    val s1234 = union(s123, singletonSet(4))
    val s1to3Not4 = filter(s1234, x => x <= 3)

    assert(s1to3Not4(1) == true)
    assert(s1to3Not4(3) == true)

    assert(s1to3Not4(0) == false)
    assert(s1to3Not4(-1) == false)
    assert(s1to3Not4(4) == false)
    assert(s1to3Not4(5) == false)
  }

  "forall(s123, x => x < 5 && x >= 1)" should "check all elements" in {
    val s123 = union(union(s1, s2), s3)

    assert(forall(s123, x => x < 5 && x >= 1) == true)
    assert(forall(s123, x => x < 5 && x >= 2) == false)
    assert(forall(s123, x => x < 3 && x >= 1) == false)
    assert(forall(s123, x => x < 3 && x > 1) == false)
  }

  "exists(s123, x == 1)" should "check whether the element exists or not" in {
    val s123 = union(union(s1, s2), s3)

    assert(exists(s123, x => x >= 1) == true)
    assert(exists(s123, x => x < 5 && x >= 3) == true)
    assert(exists(s123, x => x < 5 && x >= 4) == false)
  }

  "map(s123, (x: Int) => x * 2)" should "apply the predicate to all elems" in {
    val s123 = union(union(s1, s2), s3)
    val s246 = map(s123, x => x * 2)

    assert(forall(s246, x => x % 2 == 0) == true)
    assert(contains(s246, 2) == true)
    assert(contains(s246, 4) == true)
    assert(contains(s246, 6) == true)
  }
}
