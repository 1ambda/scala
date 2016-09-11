package st

import org.scalatest.{Matchers, FunSuite}

import scalaz.Forall

/** https://github.com/scalaz/scalaz/blob/series/7.3.x/example/src/main/scala/scalaz/example/STUsage.scala */
class STSpec extends FunSuite with Matchers {
  test("ST usage 1") {
    import scalaz._
    import effect._
    import ST._

    // Creates a new mutable reference and mutates it
    def e1[A] = for {
      r <- newVar[A](0)
      x <- r.mod(_ + 1)
    } yield x

    // Creates a new mutable reference, mutates it, and reads its value.
    def e2[A] = e1[A].flatMap(_.read)

    def e3[A] = e1[A].flatMap(ref => ref.mod(_ + 10))

    // Run e2, returning the final value of the mutable reference.
    def test1 = new Forall[({type λ[S] = ST[S, Int]})#λ] {
      def apply[A] = e2
    }

    def test2 = new Forall[({type λ[S] = ST[S, Int]})#λ] {
      def apply[A] = e3.flatMap(_.read)
    }

    // Run e1, returning a mutable reference to the outside world.
    // The type system ensures that this can never be run.
    def test3 = new Forall[({type λ[S] = ST[S, STRef[S, Int]]})#λ] {
      def apply[A] = e1
    }

    val compiles = runST(test2)

    println(compiles)
  }
}
