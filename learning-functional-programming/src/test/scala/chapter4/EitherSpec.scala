package chapter4

import org.scalatest.{Matchers, FunSuite}

import scala._
import scala.util.Try

class EitherSpec extends FunSuite with Matchers {
  import EitherExample._

  test("scala either test") {
    val l: scala.Either[String, Int] = scala.Left("flower")
    val r: scala.Either[String, Int] = scala.Right(12)

    l.left.map(_.size) should be (scala.Left(6))
    r.left.map(_.size) should be (scala.Right(12))

    l.right.map(_.toDouble) should be (scala.Left("flower"))
    r.right.map(_.toDouble) should be (scala.Right(12.0))
  }

  test("custom either test: mean function") {
    val empty = IndexedSeq()
    val notEmpty = IndexedSeq(1.0, 2.0, 3.0)

    mean(empty) should be (Left("seq is empty"))
    mean(notEmpty) should be (Right(2.0))
  }

  test("Try test") {
    val a = "flower"
    val b = "32"

    Either.Try(b.toInt) should be (Right(32))
  }
}
