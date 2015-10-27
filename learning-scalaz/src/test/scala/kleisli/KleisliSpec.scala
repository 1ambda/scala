package kleisli

import org.scalatest.{Matchers, FunSuite}

import scalaz._, Scalaz._, Kleisli._

import scala.util._

class KleisliSpec extends FunSuite with Matchers {

  /**
   * http://www.leonardoborges.com/writings/2014/06/17/functional-composition-with-monads-kleisli-functors/
   * https://github.com/scalaz/scalaz/blob/c847654dcf75c748eacbaf246511bbd938b8631f/core/src/main/scala/scalaz/Kleisli.scala
   * http://eed3si9n.com/learning-scalaz/Composing+monadic+functions.html
   * http://underscore.io/blog/posts/2015/10/14/reification.html
   */

  /**
   * Kleisli represents a function `A => M[B]`.
   *
   * final case class Kleisli[M[_], A, B](run: A => M[B]) { self =>
   *
   *  ...
   * }
   */

  // ref - http://www.leonardoborges.com/writings/2014/06/17/functional-composition-with-monads-kleisli-functors/
  test("example 0") {

    type ID = String
    type Table = String

    val ids = List("1ambda", "2ambda")
    val tables = List("product", "coupon", "user")
    val owner: Map[ID, List[Table]] = Map(
      "1ambda" -> List("product, user"),
      "2ambda" -> List("user")
    )

    case class Authentication(id: ID)
    case class Authorization(id: ID, table: Table)

    def authenticate: ID => Option[Authentication] =
      (id: ID) => (ids contains id).option(Authentication(id))

    def authorize: Authentication => List[Authorization] =
      (auth: Authentication) => owner.getOrElse(auth.id, List()).map { table =>
        Authorization(auth.id, table)
      }

    val authorizations1 = kleisli(authenticate) flatMapK authorize
    val authorizations2 = authenticate(_: ID) map authorize getOrElse(Nil)
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

  /* ref - https://github.com/scalaz/scalaz/blob/series/7.2.x/example/src/main/scala/scalaz/example/KleisliUsage.scala */
  test("example 2") {
    case class Continent(name: String, countries: List[Country] = List.empty)
    case class Country(name: String, cities: List[City] = List.empty)
    case class City(name: String, isCapital: Boolean = false, inhabitants: Int = 20)

    val data: List[Continent] = List(
      Continent("Europe"),
      Continent("America",
        List(
          Country("Canada",
            List(
              City("Ottawa"), City("Vancouver"))),
          Country("USA",
            List(
              City("Washington"), City("New York"))))),
      Continent("Asia",
        List(
          Country("India",
            List(City("New Dehli"), City("Calcutta"))))))


    def continents(name: String): List[Continent] =
      data.filter(k => k.name.contains(name))

    def countries(continent: Continent): List[Country] = continent.countries

    def cities(country: Country): List[City] = country.cities

    def save(cities: List[City]): Try[Unit] =
      Try {
        cities.foreach(c => println("Saving " + c.name))
      }

    def inhabitants(c: City): Int = c.inhabitants

    // Kleisli[List, String, City]
    val allCities = kleisli(continents) >==> countries >==> cities

    // Kleisli[List, String, Int]
    val cityInhabitants = allCities map inhabitants

    allCities("America") map(println)
    (allCities =<< List("America", "Asia")).map(println)

    // Kleisli[Try, String, Unit]
    val getAndSaveCities = allCities mapK save

    def index(i: Int): String = data(i).name

    // Kleisli[List, Int, City]
    val allCitiesWithIndex = allCities local index

    allCitiesWithIndex(1) map(println)

  }

}

