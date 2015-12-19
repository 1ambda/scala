package json

import util.TestSuite

/**
 * ref - https://skillsmatter.com/skillscasts/6875-workshop-shapeless-for-mortals
 */
class ShapelessJsonSpec extends TestSuite {

  import shapeless._, labelled._, syntax.singleton._

  test("singleton types") {
    "bar".narrow shouldBe "bar" // <: String
    // 'foo.narrow shouldBe 'foo /* symbol */
    true.narrow shouldBe true
    Nil.narrow shouldBe Nil // Nil.type

    'a ->> "bar" // : String with KeyTag[Symbol('a), String
    'b ->> 42    // : Int    with KeyTag[Symbol('b), Int

    val foo = Witness[String]("foo").value
    val answer = Witness[Int](42).value

    // field[Symbol('a)]("bar") : FieldType[Symbol('a), String]
  }

  test("HList") {
    val h1 = "hello" :: 13L :: true :: HNil
    val h2 = ('a ->> "hello") :: ('b ->> 13L) :: ('c ->> true) :: HNil

    /**
     * FieldType[Symbol('a), String] ::
     * FieldType[Symbol('b), Int] ::
     * FieldType[Symbol('c), Boolean] :: HNil
     */
  }

}
