import util.TestSuite

class SingletonTypeSepc extends TestSuite {

  import shapeless._, labelled._, syntax.singleton._

  test("narrow") {
    "bar".narrow shouldBe "bar" // String("bar") <: String
    'foo.narrow // Symbol('foo) <: Symbol
    Nil.narrow
  }

  test("KeyTag") {
    // String with KeyTag[Symbol('a), String]
    'a ->> "bar" shouldBe "bar"

    // Int    with KeyTag[Symbol('b), Int]
    'b ->> 42 shouldBe 42

    // since we can not use Symbol('a) as a type parameter like `field[Symbol('a')]`, starts from value
    val a = Witness('a)
    val bar = field[a.T]("bar")

    bar shouldBe bar
  }

  /** singleton types bridge the gap between the value level and the type level */
  test("Singleton-typed literals") {
    /**
      * wTrue: shapeless.Witness{type T = Boolean(true)}
      * wFalse: shapeless.Witness{type T = Boolean(false)}
      */
    val (wTrue, wFalse) = (Witness(true), Witness(false))

    type True = wTrue.T
    type False = wFalse.T

    trait Select[B] { type Out }

    implicit val selectInt = new Select[True] { type Out = Int }
    implicit val selectString = new Select[False] { type Out = String }

    def select(b: WitnessWith[Select])(t: b.instance.Out) = t

    select(true)(23) shouldBe 23
    select(false)("foo") shouldBe "foo"
  }
}
