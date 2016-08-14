package cats

import util.TestSuite

class KindProjectorSpec extends TestSuite {

  /**
    * ref - http://stackoverflow.com/questions/9443004/what-does-the-operator-mean-in-scala
    *
    * When you declare a class inside another class in Scala,
    * you are saying that each instance of that class has such a subclass
    *
    * In other words, there's no `A.B` class,
    * but there are `a1.B` and `a2.B` classes,
    * and the are different classes, as the error message is telling us above.
    */

  class A {
    class B

    def f1(b: B) = println("Got my B")
    def f2(b: A#B) = println("Got A#B")
  }

  test("Type Projection") {

    val a1 = new A
    val a2 = new A

    /** can't compile:
      * a1.f1(new a2.B)
      */

    /**
      * '#' makes it possible to refer such nested classes without restricting it to a particular instance
      */
    a1.f2(new a2.B)
  }

  test("Kind Projector") {

    /** Using type projections to implement anonymous,
      * partially applied types is annoying in Scala
      */
    def foo[T] = ()
    def bar[T[_]] = ()
    def baz[T[_, _]] = ()

    type ?? = Unit
    type IntOrA[A] = Either[A, Int]

    /** use type alias */
    foo[??]
    bar[IntOrA]

    /** use type lambda */
    bar[({ type L[X] = Either[Int, X] })#L]

    /** use kind-projector */
    bar[Either[Int, ?]]
    baz[Tuple3[Int, ?, ?]]

    /** use Lambda syntax for nested types */
    bar[Lambda[A => (A, A)]]    // equiv to: type R[A] = (A, A)
    baz[Lambda[(A, B) => (A, B, A)]] // equiv to: Type R[A, B] = (A, B, A)

    /** xyz */
    type Fake[A] = A
    foo[Fake[(Int, Double) => Either[Double, Int]]]
    baz[Lambda[(A, B) => Either[B, A]]]

  }

}
