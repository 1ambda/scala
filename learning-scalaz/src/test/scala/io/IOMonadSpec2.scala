package io

import org.scalatest.{Matchers, FunSuite}

class IOMonadSpec2 extends FunSuite with Matchers {
  import IO._

  test("IO example") {
    val program: IO[Unit] = for {
      c1 <- getChar
      c2 <- getChar
      _ <- putChar(c1)
      _ <- putChar(c2)
    } yield ()
  }
}

// ref https://gist.github.com/tonymorris/7328537
object IO {
  import Operation._

  sealed trait IO[A] {
    def map[B](f: A => B): IO[B] = this match {
      case Done(a) => Done(f(a))
      case More(a) => More(a map (_ map f))
    }
    def flatMap[B](f: A => IO[B]): IO[B] = this match {
      case Done(a) => f(a)
      case More(a) => More(a map (_ flatMap f))
    }
  }

  case class Done[A](a: A)                extends IO[A]
  case class More[A](a: Operation[IO[A]]) extends IO[A]

  def putChar(c: Char): IO[Unit] = More(PutChar(c, Done()))
  def getChar: IO[Char] = More(GetChar(Done(_)))
}

object Operation {

  sealed trait Operation[A] {
    def map[B](f: A => B): Operation[B] = this match {
      case PutChar(c, a)  => PutChar(c, f(a))
      case GetChar(g)     => GetChar(f compose g)
    }
  }

  case class PutChar[A](c: Char, a: A)  extends Operation[A]
  case class GetChar[A](f: Char => A)   extends Operation[A]
}
