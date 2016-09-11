package labelled

import util.TestSuite

/**
 * - https://github.com/milessabin/shapeless/blob/master/examples/src/main/scala/shapeless/examples/labelledgeneric.scala
 */
class LabelledGenericSpec extends TestSuite {

  import shapeless._
  import record._
  import ops.record._
  import syntax.singleton._

  case class Book(author: String, title: String, id: Int, price: Double)
  case class ExtendedBook(author: String, title: String, id: Int, price: Double, inPrint: Boolean)


  test("LabelledGeneric") {
    val bookGen    = LabelledGeneric[Book]
    val bookExtGen = LabelledGeneric[ExtendedBook]

    val tapl = Book("Benjamin Pierce", "Types and Programming Language", 262162091, 44.11)
    Witness

    val repr = bookGen.to(tapl)

    repr('price) shouldBe tapl.price

    val updated = bookGen.from(repr.updateWith('price)(_+2.0))
    updated.price shouldBe (tapl.price + 2.0)

    val extended = bookExtGen.from(repr + ('inPrint ->> true))
    extended.inPrint shouldBe true

    case class Libro(autor: String, `título`: String, id: Int, precio: Double)

    println(Keys[bookGen.Repr])

  }
}
