package free

import free.withoutScalaz.{Suspend, Return, TailRecursive}
import org.scalatest._

class TailRecursiveSpec extends FunSuite with Matchers {

  test("TailRecursive") {
    val s1 = Suspend(() => { println(1); 1 })
    val s2 = Suspend(() => { println(2); 2 })

    val program1 = for {
      x <- s1
      y <- s2
    } yield x + y

    TailRecursive.run(program1) shouldBe 3
  }

  test("Stackoverflow") {
    val f = (x: Int) => x
    val g = List.fill(100000)(f).foldLeft(f)(_ compose _)
    intercept[StackOverflowError] {
      g(42)
    }

  }

  test("TailRecursive uses heap instead of stack") {
    val f: Int => TailRecursive[Int] = (x: Int) => Return(x)
    val g = List.fill(100000)(f).foldLeft(f) { (a, b) =>
      // TODO
      x => Suspend(() => ()).flatMap(_ => a(x).flatMap(b))
    }
  }
}
