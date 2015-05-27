package scalaz_either

import org.scalatest.{Matchers, FunSuite}
import scalaz._
import Scalaz._

// Ref1: https://vimeo.com/128466885
// Ref2: http://eed3si9n.com/learning-scalaz/Applicative+Builder.html
// Ref3: https://github.com/scalaz/scalaz/blob/series/7.2.x/core/src/main/scala/scalaz/Either.scala
class EitherTest extends FunSuite with Matchers {

  test("applicative builder test") {
    val result = (3.some |@| 5.some) { _ + _}
    result should be (8.some)
  }

  test("scala standard either test") {

  }

}
