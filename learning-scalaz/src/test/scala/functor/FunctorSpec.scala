package functor

import org.scalatest.{Matchers, FlatSpec}

import scalaz.Functor
import scalaz.std.option._
import scalaz.syntax.functor._
import scalaz.std.list._
import scalaz.syntax.equal._
import scalaz.concurrent.Task

// ref: https://github.com/scalaz/scalaz/blob/series/7.2.x/example/src/main/scala/scalaz/example/FunctorUsage.scala
class FunctorSpec extends FlatSpec with Matchers {

  val len: String => Int = _.length

  "A Option" should "be a functor" in {
    Functor[Option].map(Some("asdf"))(len) shouldEqual Some(4)
    Functor[Option].map(None)(len) shouldEqual None
  }

  "A List" should "be a functor" in {
    Functor[List].map(List("qwer", "asdfg"))(len) shouldBe List(4, 5)
  }

  "Functor" can "be used with lift" in {
    val lenOption: Option[String] => Option[Int] = Functor[Option].lift(len)

    lenOption(Some("abcd")) shouldBe Some(4)
  }

  "Functor" should "provides strengthL, R functions" in {
    Functor[List].strengthL("a", List(1, 2, 3)) shouldBe List("a" -> 1, "a" -> 2, "a" -> 3)
    Functor[List].strengthR(List(1, 2, 3), "a") shouldBe List(1 -> "a", 2 -> "a", 3 -> "a")

    List(1, 2, 3).strengthL("a") shouldBe List("a" -> 1, "a" -> 2, "a" -> 3)
    List(1, 2, 3).strengthR("a") shouldBe List(1 -> "a", 2 -> "a", 3 -> "a")
  }

  "Functor" should "provides a fproduct function" in {
    val source = List("a", "aa", "b", "ccccc")
    val result = List("a" -> 1, "aa" -> 2, "b" -> 1, "ccccc" -> 5)

    source.fproduct(len) shouldBe result
  }

  "Functor" should "provides void" in {
    Functor[Option].void(Some(1)) shouldBe Some()
  }

  "Functor.void" can "execute the side-effects" in {
    // pretend this is our database
    var database = Map("abc" -> 1, "aaa" -> 2, "qqq" -> 3)

    def deleteRow(f: String => Boolean): Task[Int] = Task.delay {
      val (count, db) = database.foldRight(0 -> List.empty[(String, Int)]) {
        case ((key, value), (deleted, result)) if f(key) => (deleted + 1, result)
        case (row, (deleted, result)) => (deleted, row :: result)
      }

      database = db.toMap
      count
    }

    val deleteTask = deleteRow(_.startsWith("a"))

    // it hasn't run yet
    database.size shouldBe 3

    // same as, Functor[Task].void(deleteTask)
    val voidTask: Task[Unit] = deleteTask.void

    voidTask.run shouldBe ()

    database.size shouldBe 1
  }

  "Functor" can "be albe to be composed" in {
    val listOpt = Functor[List] compose Functor[Option]
    listOpt.map(List(Some(1), None, Some(3)))(_ + 1) shouldBe List(Some(2), None, Some(4))
  }
}
