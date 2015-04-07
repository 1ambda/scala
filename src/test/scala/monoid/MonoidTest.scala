package monoid

import org.scalatest._

// ref: http://eed3si9n.com/learning-scalaz/sum+function.html
class MonoidTest extends FlatSpec with Matchers {

  trait Monoid[A] {
    def mappend(a1: A, a2: A): A
    def mzero: A
  }

  object Monoid {
    implicit val IntMonoid: Monoid[Int] = new Monoid[Int] {
      def mappend(a: Int, b: Int) = a + b
      def mzero = 0
    }

    implicit val StringMonoid: Monoid[String] = new Monoid[String] {
      def mappend(a: String, b: String) = a + b
      def mzero = ""
    }
  }

  def sum[A: Monoid](xs: List[A]): A = {
    val m = implicitly[Monoid[A]]
    xs.foldLeft(m.mzero)(m.mappend)
  }

  "sum(List(\"a\", \"b\", \"c\"))" should "be \"abc\"" in  {
    val expected = sum(List("a", "b", "c"))

    expected should be ("abc")
  }
}
