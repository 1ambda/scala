package io

import org.scalatest.{FunSuite, Matchers}

class IOMonadSpec extends FunSuite with Matchers {
  test("Pure usage1") {
    val io = for {
      _ <- Pure.println("Starting work now")
      x = 1 + 2 + 3
      _ <- Pure.println("All done.")
    } yield x

    def run = io.run
  }
}


object Pure {

  sealed trait IO[A] {
    def flatMap[B]  (f: A => IO[B]):  IO[B] = Suspend(() => f(this.run))
    def map[B]      (f: A => B):      IO[B] = Return(() => f(this.run))
    def run: A = this match {
      case Return(a) => a()
      case Suspend(s) => s().run
    }
  }

  final case class Return[A]  (a: () => A)      extends IO[A]
  final case class Suspend[A] (s: () => IO[A])  extends IO[A]

  object IO {
    def point[A](a: => A): IO[A] = Return(() => a)
  }

  def println(message: String): IO[Unit] = IO.point(Predef.println(message))

}
