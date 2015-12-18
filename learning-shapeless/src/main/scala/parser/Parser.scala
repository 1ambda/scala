package parser

import scala.reflect.ClassTag
import scala.util.Try

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

object CaseClassParser {
  import shapeless._

  trait Parser[A] {
    def apply(s: String): Option[A]
  }

  def apply[A](s: String)(implicit P: Parser[A]): Option[A] = P(s)


  implicit val intParser = new Parser[Int] {
    override def apply(s: String): Option[Int] = Try(s.toInt).toOption
  }

  implicit val stringParser = new Parser[String] {
    override def apply(s: String): Option[String] = Some(s)
  }

  implicit val doubleParser = new Parser[Double] {
    override def apply(s: String): Option[Double] = Try(s.toDouble).toOption
  }

  implicit val hnilParser = new Parser[HNil] {
    override def apply(s: String): Option[HNil] =
      if (s.isEmpty) Some(HNil) else None
  }

  implicit def hlistParser[H : Parser, T <: HList : Parser] = new Parser[H :: T] {
    override def apply(s: String): Option[H :: T] =
      s.split(",").toList match {
        case cell +: rest /* use `+:` instead of :: */ => for {
          head <- implicitly[Parser[H]].apply(cell)
          tail <- implicitly[Parser[T]].apply(rest.mkString(","))
        } yield head :: tail
      }
  }

  implicit def caseClassParser[C, R <: HList]
  (implicit G: Generic.Aux[C, R], reprParser: Parser[R]): Parser[C] = new Parser[C] {
    override def apply(s: String): Option[C] = reprParser.apply(s).map(G.from)
  }
}
