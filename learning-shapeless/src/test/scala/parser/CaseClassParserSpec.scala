package parser

import util.TestSuite

import shapeless._
import scala.reflect.ClassTag
import scala.util._


/** ref
  * - https://meta.plasm.us/posts/2015/11/08/type-classes-and-generic-derivation/
  */
class CaseClassParserSpec extends TestSuite {
  case class Person(name: String, age: Double)
  case class Book(title: String, author: String, year: Int)
  case class Country(name: String, population: Int, area: Double)

  test("Reflective Parser") {
    ReflectiveParser[Person]("Amy,54")
  }


  test("Generic Parser") {
    import GenericParser._
    GenericParser[Person]("Amy,54.2") shouldBe Some(Person("Amy", 54.2))
    GenericParser[Person]("Amy,54.2,35") shouldBe None

    trait Foo
    // GenericParser[Foo]("Hamlet,Shakespeare,1600") // doesn't compile
  }

}

object GenericParser {
  import shapeless._

  trait Parser[A] {
    def apply(s: String): Option[A]
  }

  implicit val stringParser: Parser[String] = new Parser[String] {
    override def apply(s: String): Option[String] = Some(s)
  }

  implicit val intParser: Parser[Int] = new Parser[Int] {
    override def apply(s: String): Option[Int] = Try(s.toInt).toOption
  }

  implicit val doubleParser: Parser[Double] = new Parser[Double] {
    override def apply(s: String): Option[Double] = Try(s.toDouble).toOption
  }

  implicit val hnilParser: Parser[HNil] = new Parser[HNil] {
    override def apply(s: String): Option[HNil] = if (s.isEmpty) Some(HNil) else None
  }

  // See, context bound
  // http://stackoverflow.com/questions/4465948/what-are-scala-context-and-view-bounds
  implicit def hconsParser[H: Parser, T <: HList: Parser]: Parser[H :: T] =
    new Parser[H :: T] {
      override def apply(s: String): Option[H :: T] = s.split(",").toList match {
        case cell +: rest => for {
          head <- implicitly[Parser[H]].apply(cell)
          tail <- implicitly[Parser[T]].apply(rest.mkString(","))
        } yield head :: tail
      }
    }

  implicit def caseClassParser[A, R <: HList]
  (implicit gen: Generic.Aux[A, R], reprParser: Parser[R]): Parser[A] =
    new Parser[A] {
      def apply(s: String): Option[A] = reprParser.apply(s).map(gen.from)
    }

  def apply[A](s: String)(implicit parser: Parser[A]): Option[A] = parser(s)
}

object ReflectiveParser {
  def apply[T: ClassTag](s: String): Try[T] = Try {
    val constructor = implicitly[ClassTag[T]].runtimeClass.getConstructors.head
    val paramNames = s.split(",").map(_.trim)
    val paramNamesWithTypes = paramNames.zip(constructor.getParameterTypes)

    val params = paramNamesWithTypes.map {
      case (name, clazz) => clazz.getName match {
        case "int" => name.toInt.asInstanceOf[Object]
        case "double" => name.toDouble.asInstanceOf[Object]
        case _ =>
          val paramConstructor = clazz.getConstructor(name.getClass)
          paramConstructor.newInstance(name).asInstanceOf[Object]
      }
    }

    constructor.newInstance(params: _*).asInstanceOf[T]
  }
}
