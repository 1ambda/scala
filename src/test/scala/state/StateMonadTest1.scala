package state

import org.scalatest._
import scalaz._
import Scalaz._

class StateMonadTest1 extends FlatSpec with Matchers {

  type Stack = List[Int]

  def pop = State[Stack, Int] {
    case x :: xs => (xs, x)
  }

  def push(a: Int) = State[Stack, Unit] {
    case xs => (a :: xs, ())
  }

  def pop2: State[Stack, Int] = for {
    now <- get[Stack]
    val (x :: xs) = now
    unit <- put(xs) // unit == (), since put return [Stack, Unit]
  } yield x

  def push2(a: Int): State[Stack, Unit] = for {
    now <- get[Stack]
    unit <- put(a :: now)
  } yield unit

  "Push(3) pop pop on List(5, 1, 2, 4)" should "return (List(1, 2, 4), 5)" in  {
    val s: Stack = List(5, 1, 2, 4)

    val ops: State[Stack, Int] =
      for {
        _ <- push2(3)
        a <- pop2
        b <- pop2
      } yield b

    val result = ops(s)
    result should be (List(1, 2, 4), 5)
  }

  "put List(8, 3, 1) on List(1, 2, 3)" should "return (List(8 ,3, 1), ())" in  {

    val ops = for {
      now <- get[Stack]
      unit <- put(List(8, 3, 1))
    } yield unit 

    ops(List(1, 2, 3)) should be ((List(8, 3, 1), ()))
  }
}

