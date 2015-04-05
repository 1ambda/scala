package polymorphism

import org.scalatest._

class PolymorphismTest extends FlatSpec with Matchers {

  "F-bounded polymorphism example" should "be compiled" in  {
    trait Container[A <: Container[A]] extends Ordered[A] 
    class MyContainer extends Container[MyContainer] {
      def compare(that: MyContainer) = 0
    }

    val first = new MyContainer
    val cs = List(first, new MyContainer, new MyContainer)
    cs.min should be (first)
  }

  "subtype polymorphism" should "be compiled" in {
    trait Plus[A] {
      def plus(a2: A): A
    }

    def plus[A <: Plus[A]](a1: A, a2: A) = a1.plus(a2)

    case class FooPlus(i: Int) extends Plus[FooPlus] {
      def plus(a2: FooPlus): FooPlus = FooPlus(i + a2.i)

    }

    val expected = FooPlus(3)
    val result = plus(FooPlus(2), FooPlus(1))

    result should be (expected)
  }

  "ad-hoc polymorphism" should "be compiled" in {
    trait Plus[A] {
      def plus(a1: A, a2: A): A 
    }

    // use context bound 
    def plus[A: Plus](a1: A, a2: A): A = implicitly[Plus[A]].plus(a1, a2)

    implicit val StringPlus = new Plus[String] { 
      def plus(a1: String, a2: String) = a1 + "+" + a2
    }

    val expected = "1+2"
    val result = plus("1", "2")

    result should be (expected)
  }

}
