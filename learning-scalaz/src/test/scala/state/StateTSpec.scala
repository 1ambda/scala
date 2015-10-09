package state

import org.scalatest.{FunSuite, Matchers}

import scalaz.Scalaz._
import scalaz._


// ref: https://softwarecorner.wordpress.com/2014/12/04/scalaz-statet-monad-transformer/
// http://noelwelsh.com/programming/2013/12/20/scalaz-monad-transformers/
class StateTSpec extends FunSuite with Matchers {

  test("nesting examples") {
    type Result[+A] = String \/ Option[A]
    val r: Result[Int] = some(42).right
    r shouldBe \/-(Some(42))

    val t = for {
      option <- r
    } yield {
        for {
          value <- option
        } yield value.toString
      }

    t shouldBe \/-(Some("42"))
    r map { _ map { _.toString }} shouldBe \/-(Some("42"))
  }

  test("OptionT Basic1") {
    type Error[+A] = \/[String, A]
    type Result[A] = OptionT[Error, A]

    val r1: Result[Int] = 42.point[Result]
    val t: OptionT[Error, String] = for { value <- r1 } yield value.toString

    val result = 42.point[Result]
  }

}

