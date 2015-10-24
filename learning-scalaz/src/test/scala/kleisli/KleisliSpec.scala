package kleisli

import org.scalatest.{Matchers, FunSuite}

import scalaz._, Scalaz._, Kleisli._

class KleisliSpec extends FunSuite with Matchers {

  /**
   * http://www.leonardoborges.com/writings/2014/06/17/functional-composition-with-monads-kleisli-functors/
   * https://github.com/scalaz/scalaz/blob/c847654dcf75c748eacbaf246511bbd938b8631f/core/src/main/scala/scalaz/Kleisli.scala
   * http://eed3si9n.com/learning-scalaz/Composing+monadic+functions.html
   * http://underscore.io/blog/posts/2015/10/14/reification.html
   */

  test("example 0") {

  }

  /**
   *  ref - https://wiki.scala-lang.org/display/SW/Kleisli+Monad
   *  ref - http://www.casualmiracles.com/2012/07/02/a-small-example-of-kleisli-arrows/
   *
   *  Kleisli is function composition for monad
   *  If you can have functions that return kinds of things, like Lists, Options, etc,
   *  then you can use a Kleisli to compose those functions
   *
   */

  test("example 1") {
    type Line = String
    type Field = String

    def getLines(s: String): List[Line] = s.split("\n").toList
    def getFields(l: Line): List[Field] = l.split("\t").toList

    /**
     * val f: String => List[Line]
     * val g: Line   => List[Field]
     *
     *  we can't compose getLines with getFields using flatMap because they are functions
     *  `getLines flatMap getFields`
     *
     *  instead, we can use Kelisli
     */

    def str(x: Int): Option[String] = Some(x.toString)
    def toInt(x: String): Option[Int] = Some(x.toInt)
    def double(x: Int): Option[Double] = Some(x * 2)
    val funky = kleisli(str _) >==> (toInt _) >==> (double _)

    val s = "1\t2\t3\n1\t2\t3"
    val h = kleisli(getLines) >==> (getFields)
    h(s) shouldBe "123123".toList.map(_.toString)
  }

}
