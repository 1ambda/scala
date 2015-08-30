package chapter8

// ref https://github.com/fpinscala/fpinscala/blob/master/answerkey/testing/11.answer.scala
case class SGen[A](g: Int => Gen[A]) {

  def apply(n: Int): Gen[A] = g(n)

  def map[B](f: A => B): SGen[B] =
    SGen(g andThen (_ map f))

  def flatMap[B](f: A => Gen[B]): SGen[B] =
    SGen(g andThen (_ flatMap f))

  def **[B](s2: SGen[B]): SGen[(A,B)] =
    SGen(n => apply(n) ** s2(n))
}

