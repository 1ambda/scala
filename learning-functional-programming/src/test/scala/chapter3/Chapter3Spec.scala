package chapter3

import org.scalatest.{FunSuite, Matchers}


class Chapter3Spec extends FunSuite with Matchers {

  test("List.sum, List.product") {
    val b = List(1.0, 0.0, 2.0, 3.0)
    val c = List(1.0, 2.0, 3.0, 4.0)
    val a = List(1, 2, 3, 4)

    List.sum(a) should be (10)
    List.product(b) should be (0.0)
    List.product(c) should be (24.0)
  }

  test("has") {
    val a = List(1, 2, 3)

    List.has(a)(1) should be (true)
    List.has(a)(4) should be (false)
  }

  test("dropWhile") {
    val a = List(1, 2, 3, 4, 5)
    List.dropWhile(a)(_ < 4) should be (List(4, 5))
  }

  test("tail") {
    val a = List(1, 2, 3, 4, 5)
    val b = List.tail(a)
    val c = List.tail(b)

    b should be (List(2, 3, 4, 5))
    c should be (List(3, 4, 5))
  }

  test("drop") {
    val a = List(1, 2, 3, 4, 5)

    List.drop(a)(0) should be (a)
    List.drop(a)(3) should be (List(4, 5))
    List.drop(a)(6) should be (Nil)
  }

  test("init") {
    val a = List(1, 2, 3, 4, 5)

    List.init(a) should be (List(1, 2, 3, 4))
    List.init(Nil) should be (Nil)
    List.init(List(1)) should be (List())
  }
}



