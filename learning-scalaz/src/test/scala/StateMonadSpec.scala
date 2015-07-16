import org.scalatest.{Matchers, FunSuite}

import scala.concurrent.Future
import scalaz._, Scalaz._
import scala.io.Source

/*

  ref: https://softwarecorner.wordpress.com/2014/12/04/scalaz-statet-monad-transformer/

  It's important to realize that this function is not a method on State but
  rather a data field within the State object.

  That is, different State objects can have different functions inside them.

 */

class StateMonadSpec extends FunSuite with Matchers{

  def len = State { s: String => (s, s.size) }
  def repeat(num: Int): State[String, Unit] = State { s: String => (s * num, ()) }

  test("State test") {
    len.run("hello") shouldBe ("hello", 5)
    repeat(3).run("hello") shouldBe ("hello" * 3, ())
  }

  // def flatMap[B](f: A => State[S, B]): State[S, B]
  test("flatMap test") {
    len.flatMap(repeat).run("hello") shouldBe ("hello" * 5, ())

    len.flatMap(repeat).flatMap(_ => len).run("hello") shouldBe ("hello" * 5, 25)
  }

  // def get[S] = State { s: S => (s, s) }
  // def map[B](f: A => B): State[S, B]
  test("get, map test") {
    get[String]
      .flatMap(s0 => repeat(s0.size))
      .flatMap(_ => get[String])
      .map(s1 => s1.size)
      .run("hello") shouldBe ("hello" * 5, 25)
  }

  // def put[S](newState: S): State { s: S => (newState, ()) }
  test("get, put") {
    get[String]
      .flatMap(s0 => put(s0 * s0.size))
      .flatMap(_ => get[String])
      .map(s1 => s1.size)
      .run("hello") shouldBe ("hello" * 5, 25)
  }

  test("for comprehension") {
    val m = for {
      s0 <- get[String]
      _ <- put(s0 * s0.size)
      s1 <- get[String]
    } yield s1.size

    m.run("hello") shouldBe ("hello" * 5, 25)
  }


  /*
    In for comprehension solution, it fetches the current state and stores it in s0.
    Then it changes the state with the call to put(), meaning that the state in s0 is now obsolete.
    But s0 is still in scope. So, we can continue to use th state in s0. even though it's obsolete.

    It would be easy to accidentally yield s0.size instead of s1.size

    So, we need to use modify instead of put.

    def modify[S](f: S => S) = State { s: S => (f(s), ()) }
   */

  test("modify test") {
    val m = for {
      _ <- modify { s: String => s * s.size}
      s1 <- get[String]
    } yield s1.size

    m.run("hello") shouldBe ("hello" * 5, 25)
  }

  /*
    We can get ride of s1 using functions gets()

    def gets[S, A](f: S => A) = State { s: S => (s, f(s)) }
   */

  test("gets test") {
    val m = for {
      _ <- modify { s: String => s * s.size }
      size <- gets { s: String => s.size }
    } yield size

    m.run("hello") shouldBe ("hello" * 5, 25)
  }

  lazy val buildFile: String = {
    val f = Source.fromFile("build.sbt")
    try f.getLines().mkString("\n") finally f.close
  }

  lazy val wordList = buildFile.split("\n").toList
    .flatMap(s => s.split(" "))
    .filter(s => s != "")

  def words(str: String) = wordList.filter(_.contains(str))

  // ref: https://softwarecorner.wordpress.com/2013/08/29/scalaz-state-monad/
  test("wordcount example 1") {
    def wordCounts(str: String, currMap: Map[String, Int]): Map[String, Int] = {
      words(str).foldLeft(currMap) { (map, word) =>
        val count = map.getOrElse(str, 0) + 1
        map + (str-> count)
      }
    }

    val m = wordCounts("scalaz", Map.empty[String, Int])
    m.size should not be 0
  }

  test("wordCount example 2") {
    def wordCount(str: String): State[Map[String, Int], Unit] =
      modify { currMap: Map[String, Int] =>
        words(str).foldLeft(currMap) { (map, word) =>
          val count = map.getOrElse(str, 0) + 1
          map + (str-> count)
        }
      }

    val m = for {
      _ <- wordCount("scalaz")
      _ <- wordCount("org")
      _ <- wordCount("version")
    } yield ()

    val (wordMap, _) = m.run(Map.empty[String, Int])
  }

  test("wordCount example 3") {

  }
}
