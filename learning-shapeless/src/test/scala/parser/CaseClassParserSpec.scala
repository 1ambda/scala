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
    // doesn't work
    ReflectiveParser[Person]("Amy,54")
  }

  test("Generic Parser") {
    import CaseClassParser._
    CaseClassParser[Person]("Amy,54.2") shouldBe Some(Person("Amy", 54.2))
    CaseClassParser[Person]("Amy,54.2,35") shouldBe None

    trait Foo
    // GenericParser[Foo]("Hamlet,Shakespeare,1600") // doesn't compile
  }
}

