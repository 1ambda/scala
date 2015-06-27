package chapter5

import org.scalatest.{Matchers, FunSuite}

class StreamSpec extends FunSuite with Matchers {
  import Stream._

  test("empty stream") {
    Stream.empty should be (Empty)
  }

  test("stream creation") {
    val s1 = Cons(() => println(3), () => Empty)
    val s2 = Stream.cons2(println(3), Empty)
  }

  test("test invalid cons impl") {
    val s = Stream.cons2(println("twice"), Empty)
    s.h() // evaluate head every time
    s.h() // evaluate head every time
  }

  test("test valid cons impl") {
    val s = Stream.cons(println("once"), Empty)
    s.h()
    s.h()
  }

  test("Stream.apply test") {
    // no lazy evaluation
    // https://github.com/fpinscala/fpinscala/issues/321
    val s1 = Stream(println("first"), println("second"), println("third"))
    val s2 = scala.Stream(println("first2"), println("second2"), println("third2"))


    s1 match {
      case Empty => fail
      case Cons(h, t) => h()
    }
  }
}
