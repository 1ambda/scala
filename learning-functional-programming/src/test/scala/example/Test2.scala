package example

import org.scalatest.{Matchers, FunSuite}

class Test extends FunSuite with Matchers {

  val ns = List(-1, 3, -4, 5, 1, -6, 2, 1)

  test("should return 1 or 7") {
    Solution.solution0(ns.toArray) should (equal (7) or equal (3) or equal(1))
  }

  test("problem1") {
    val s1 = "00-44  48 5555 8361"
    val s2 = "0 - 22 1985--324"
    val s3 = "555372654"
    val e1 = "004-448-555-583-61"
    val e2 = "022-198-53-24"
    val e3 = "555-372-654"

    Solution.solution1(s1) shouldBe e1
    Solution.solution1(s2) shouldBe e2
    Solution.solution1(s3) shouldBe e3
  }

  test("problem2") {
    val s1 = "13+62*7+*"
    val e1 = 76

    val s2 = "11++"
    val e2 = -1

    Solution.solution2(s1) shouldBe e1
    Solution.solution2(s2) shouldBe e2
  }

  test("problem3") {
    Solution.solution3(123) shouldBe 321
    Solution.solution3(535) shouldBe 553
  }

  test("problem4") {
    Solution.solution4(Array(1, 3, 2, 1), Array(4, 2, 5, 3, 2)) shouldBe 2
    Solution.solution4(Array(2, 1), Array(3, 3)) shouldBe -1

    Solution.solution4(Array(1), Array(3)) shouldBe -1
    Solution.solution4(Array(1), Array(1)) shouldBe 1
    Solution.solution4(Array(3), Array(1)) shouldBe -1

    Solution.solution4(Array(1, 3), Array(3, 1)) shouldBe 1
    Solution.solution4(Array(3, 1), Array(3, 1)) shouldBe 1
    Solution.solution4(Array(3, 1), Array(1, 3)) shouldBe 1

    Solution.solution4(Array(3, 5, 5), Array(3, 1)) shouldBe 3
    Solution.solution4(Array(5, 1), Array(5, 3)) shouldBe 5

    Solution.solution4(Array(5, 4, 3, 2, 1), Array(1, 2, 3, 4, 5)) shouldBe 1
    Solution.solution4(Array(1, 2, 3, 4, 5), Array(5, 4, 3, 2, 1)) shouldBe 1

    Solution.solution4(Array(1, 1, 2), Array(7, 2)) shouldBe 2
    Solution.solution4(Array(7, 2), Array(1, 1, 2)) shouldBe 2
  }
}

object Solution {

  import scala.util._
  def solution4(A: Array[Int], B: Array[Int]): Int = {
    var n = A.length // 2, 7
    var m = B.length // 1, 1, 2
    Sorting.quickSort(A)
    Sorting.quickSort(B)
    var k: Int = 0
    var i: Int = 0
    while (k < n) {
      if (i < m - 1 && B(i) < A(k)) i += 1
      if (A(k) == B(i)) return A(k)
      if (A(k) < B(i) || i == m - 1) k += 1
    }

    -1
  }

  def solution3(N: Int): Int = {
    // 기껏해봐야 42000000000 12자리 미만, 따라서 상수시간, O(1) 로 볼 수 있음
    val s: List[Int] = N.toString.grouped(1).toList.map(_.toInt).sorted(Ordering[Int].reverse)
    s.mkString.toInt
  }

  def solution2(S: String): Int = {
    if (S.length == 0) return -1

    def recur(s: List[String], ns: List[Int]): Int = s match {
      case Nil if ns.isEmpty => -1
      case Nil => ns.head
      case (o :: os) => o match {
        case "*" if ns.size > 1 =>
          val n1 = BigInt(ns.head)
          val n2 = BigInt(ns.tail.head)
          val n3: BigInt = n1 * n2
          if (n3 < Integer.MIN_VALUE || n3 > Integer.MAX_VALUE) -1
          else recur(s.tail, n3.toInt :: ns.drop(2))
        case "+" if ns.size > 1 =>
          val n1 = BigInt(ns.head)
          val n2 = BigInt(ns.tail.head)
          val n3: BigInt = n1 + n2
          if (n3 < Integer.MIN_VALUE || n3 > Integer.MAX_VALUE) -1
          else recur(s.tail, n3.toInt :: ns.drop(2))
        case str =>
          if (str == "+" || str == "*") -1
          else recur(s.tail, s.head.toInt :: ns)
      }
    }

    try {
      val ss = S.grouped(1).toList
      recur(ss, List())
    } catch {
      case e: NumberFormatException => -1
    }

  }

  def solution1(S: String): String = {

    val escaped = S.replaceAll("[-\\s]", "")

    if (escaped.length % 3 == 0) escaped.grouped(3).mkString("-")
    else if (escaped.length == 2) escaped
    else {
      val head = escaped.take(escaped.length - 2).grouped(3).mkString("-")
      val tail = "-" + escaped.drop(escaped.length - 2)

      head + tail
    }
  }

  def solution0(A: Array[Int]): Int = {
    val ns = A.toList

    val result = for {
      i <- 0 until ns.size
      if (ns.take(i).sum == ns.drop(i + 1).sum)
    } yield i

    result(0)
  }
}

