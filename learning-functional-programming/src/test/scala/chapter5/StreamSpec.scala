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

  val s = cons(1, cons(2, cons(3, cons(4, Empty))))

  test("Stream.exist test") {
    s exists (_ == 4) should be (true)
    s exists (_ > 6) should be (false)
  }

  test("Stream.forAll test") {
    s forAll (_ > 5) should be (false)
    s forAll (_ < 4) should be (false)
  }

  test("Stream.map test") {
    s.map(_ + 6).forAll(_ > 6) should be (true)
  }

  test("Stream.filter") {
    s.filter (_ > 3) exists (_ == 4) should be (true)
    s.filter (_ > 10) exists(_ => true) should be (false)

    s.filter(_ > 3).toList.size should be (1)
    s.filter(_ > 10).toList.size should be (0)
  }

  test("Stream.toList") {
    s.toList should be (List(1, 2, 3, 4))
    Empty.toList should be (Nil)
    Empty.toList should be (List())
  }

  test("Stream.append") {
    val t = cons(5, cons(6, Empty))
    val u = s.append(t)

    u.toList should be (List(1, 2, 3, 4, 5, 6))
  }

  test("Stream.flatMap") {
    val s = cons(1, cons(4, Empty))
    def g(x: Int): Stream[Int] = cons(x - 1, cons(x, cons(x + 1, Empty)))

    s.flatMap(g).toList should be (List(0, 1, 2, 3, 4, 5))
  }

  test("Stream.take") {
    val s = cons(1, cons(2, Empty))

    s.take(1).toList shouldBe List(1)
    s.take(3).toList shouldBe List(1, 2)
  }

  test("Stream.constant") {
    val fiveOnes = Stream.constant(1).take(5)

    fiveOnes.toList.size shouldBe 5
    fiveOnes.forAll(_ == 1)
  }

  test("Stream.from") {
    val fromFiveToNine = Stream.from(5).take(5)

    fromFiveToNine.toList shouldBe List(5, 6, 7, 8, 9)
  }

  test("Stream.fibs") {
    Stream.fibs.take(7).toList shouldBe List(0, 1, 1, 2, 3, 5, 8)
  }

}
