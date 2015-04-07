package monoid

import org.scalatest._

// ref: http://eed3si9n.com/learning-scalaz/FoldLeft.html
class FoldLeftTest extends FlatSpec with Matchers {

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

  trait FoldLeft[F[_]] {
    def foldLeft[A, B](xs: F[A], b: B, f: (B, A) => B): B
  }

  object FoldLeft {
    implicit val foldLeftList: FoldLeft[List] = new FoldLeft[List] {
      def foldLeft[A, B](xs: List[A], b: B, f: (B, A) => B) = xs.foldLeft(b)(f)
    }
  }

  // context bound
  def sum[M[_]: FoldLeft, A: Monoid](xs: M[A]): A = {
    val m: Monoid[A] = implicitly[Monoid[A]]
    val fl: FoldLeft[M] = implicitly[FoldLeft[M]]
    fl.foldLeft(xs, m.mzero, m.mappend)
  }

  "sum(List(\"a\", \"b\", \"c\"))" should "be \"abc\"" in  {
    val expected = sum(List("a", "b", "c"))

    expected should be ("abc")
  }

  "sum(List(1, 2, 3, 4))" should "be 10" in  {
    val expected = sum(List(1, 2, 3, 4))
    expected should be (10)
  }

  // MonoidOp
  trait MonoidOp[A] {
    val F: Monoid[A]
    val value: A
    def |+|(a2: A) = F.mappend(value, a2)
  }

  implicit def toMonoidOp[A: Monoid](a: A): MonoidOp[A] = new MonoidOp[A] {
    val F: Monoid[A] = implicitly[Monoid[A]]
    val value = a
  }

  "3 |+| 7" should "be 7" in  {
    (3 |+| 7) should be (10)
  }

  "\"a\" + \"bc\"" should "\"abc\"" in  {
    ("a" |+| "bc") should be ("abc")
  }
}
