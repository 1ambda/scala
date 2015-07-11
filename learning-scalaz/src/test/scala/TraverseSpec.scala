import org.scalatest.{FlatSpec, Matchers}

import scalaz.Traverse
import scalaz.std.list._
import scalaz.std.option._

// ref: https://github.com/scalaz/scalaz/blob/series/7.2.x/example/src/main/scala/scalaz/example/TraverseUsage.scala
class TraverseSpec extends FlatSpec with Matchers {

  "Traverse.sequence" should "turns F[G[A]] into G[F[A]]" in {
    val list1: List[Option[Int]] = List(Some(1), Some(2), Some(3), Some(4))
    val list2: List[Option[Int]] = List(Some(1), Some(2), None, Some(4))

    // The Traverse type class is the sequence operation,
    // which given a Traverse[F] and Applicative[G] turns F[G[A]] into G[F[A]].
    Traverse[List].sequence(list1) shouldBe Some(List(1, 2, 3, 4))

    // The effect of the inner Applicative used, so in the case of `Option` applicative,
    Traverse[List].sequence(list2) shouldBe None

    // syntax sugar
    import scalaz.syntax.traverse._
    list1.sequence shouldBe Some(List(1, 2, 3, 4))
    list2.sequence shouldBe None
  }

  "Traverse.traverse" should "apply functions and sequence in order" in {
    import scalaz.syntax.traverse._

    val smallNumbers = List(1, 2, 3, 4, 5)
    val bigNumbers = List(10, 20, 30, 40, 50)
    val doubleSmall: Int => Option[Int] = { x =>
      if (x < 30) Some(x * 2) else None
    }

    smallNumbers.map(doubleSmall).sequence shouldBe Some(List(2, 4, 6, 8, 10))
    smallNumbers.traverse(doubleSmall) shouldBe Some(List(2, 4, 6, 8, 10))

    bigNumbers.traverse(doubleSmall) shouldBe None
  }

}
