package chapter10

trait WordCount
case class Stub(chars: String) extends WordCount
case class Part(lStub: String, wordCount: Int, rStub: String) extends WordCount

// ref: https://github.com/fpinscala/fpinscala/blob/master/answerkey/monoids/10.answer.scala
object WordCount {
  import Monoid._

  implicit def wordCountMonoid = new Monoid[WordCount] {
    override def op(w1: WordCount, w2: WordCount): WordCount = (w1, w2) match {
      case (Stub(t1), Stub(t2)) => Stub(t1 + t2)
      case (Stub(t), Part(l, wc, r)) => Part(t + l, wc, r)
      case (Part(l, wc, r), Stub(t)) => Part(l, wc, r + t)
      case (Part(l1, wc1, r1), Part(l2, wc2, r2)) =>
        Part(l1, wc1 + (if ((r1 + l2).isEmpty) 0 else 1) + wc2, r2)
    }

    override def zero: WordCount = Stub("")
  }

  def count(s: String): Int = {
    val splited = s.split(" ")
    foldMap(splited

  }

}
