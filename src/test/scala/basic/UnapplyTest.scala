import org.scalatest._

class UnapplyTest extends FlatSpec with Matchers {

  "unapply" should "be called when an object is matched" in {
    // http://www.scala-lang.org/old/node/112

    class Person(val name: String, val age: Int)

    // An object which has unapply method is called extractor
    object Person {
      // apply is optional. you can comment out the apply method below
      // def apply(name:String, age: Int): Person = {
      //   new Person(name, age)
      // }

      def unapply(p: Person): Option[(String, Int)] = {
        if (p.name == "Bourne")
          Some("Treadstone", p.age)
        else
          Some(p.name, p.age)
      }
    }

    val person1 = new Person("Bourne", 26)

    person1 match {
      // this is where the unapply method is called
      // person1 is passed as a parameter to the unapply method of Person object
      // and values insode of parens should match with what thrown from the unapply method 
      case Person("Treadstone", age) => assert(age == 26);
      case _ => fail()
    }
  }

  // http://www.jayway.com/2011/10/11/injectors-and-extractors-in-scala/
  "IP Address" can "be parsed using unapply method" in {

    val ipList = Map(
      "211.1.251.1" -> true,
      "256.1.251.1" -> false,
      "asd.asd.asd.asd" -> false,
      "40.1.4" -> false
    )

    ipList foreach { case(ip, result) =>
      ip match {
        case IPv4Address(_, _, _, _) => assert(result)
        case _ => assert(!result)
      }
    }

    object IPv4Address {
      def unapply(ip: String): Option[(String, String, String, String)] = {

        val tokens = ip split "\\."

        if (tokens.length == 4 && isValidRange(tokens))
          Some(tokens(0), tokens(1), tokens(2), tokens(3))
        else
          None
      }

      private def isValidRange(tokens: Array[String]): Boolean = {
        tokens forall { token =>
          try {
            val number = token.toInt
            0 <= number && number <= 255
          } catch {
            case _: Throwable=> false
          }
        }
      }
    }
  }

  "unapply" can "return Boolean and Option[T]" in {

    // http://stackoverflow.com/questions/18743230/scala-unapply-that-returns-boolean
    object Uppercase {
      def unapply(s: String): Boolean = s.toUpperCase == s
    }

    object isUppercase {
      // or Option(s) 
      def unapply(s: String): Option[Boolean] = Some(s).map( x => x.toUpperCase == x )
    }


    val testMap = Map(
      "Foo" -> false,
      "FOO" -> true
    )

    testMap foreach { case(str, result) =>
      str match {
        /*
         * http://stackoverflow.com/questions/2359014/scala-operator
         * 
         * x will be "FOO" or "Foo"
         * `@` can be used to bind a name to a successfully matched pattern
         * e.g val List(x, xs @ _*) = List(1, 2, 3)
         *  x = 1
         *  xs = List(2, 3)
         */
        case x @ Uppercase() => assert(result)
        case _ => assert(!result)
      }

      str match {
        case isUppercase(flag) => assert(flag == result)
      }
    }
  }
}
