import org.scalatest._
import scalaz._, Scalaz._, Kleisli._, syntax.all._

class KleisliSpec extends WordSpec with Matchers {

  /**
   *  ref - https://wiki.scala-lang.org/display/SW/Kleisli+Monad
   *  ref - http://www.casualmiracles.com/2012/07/02/a-small-example-of-kleisli-arrows/
   *
   *  Kleisli is function composition for monad
   *  If you can have functions that return kinds of things, like Lists, Options, etc,
   *  then you can use a Kleisli to compose those functions
   *
   */

  "Kleisli" in {
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
    val result = h(s)

    getLines(s) foreach { l =>
      println(getFields(l))
    }






  }


}
