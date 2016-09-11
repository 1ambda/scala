import util.TestSuite

class AuxPatternSpec extends TestSuite {

  test("Aux Pattern Test") {
    trait Foo[A] {
      type B
      def value: B
    }

    object Foo {
      type Aux[A0, B0] = Foo[A0] { type B = B0 }

      implicit def fooInt = new Foo[Int] {
        override type B = String
        override def value: B = "Foo"
      }
    }

    def ciao[T, R](t: T)(implicit f: Foo.Aux[T, R]): R = f.value

    /** ref - https://gist.github.com/gigiigig/3cd104e8951b4432afd5 */
    ciao(3) shouldBe "Foo"
  }

}
