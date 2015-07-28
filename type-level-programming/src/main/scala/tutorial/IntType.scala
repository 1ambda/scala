package tutorial

// ref: http://downloads.typesafe.com/website/presentations/ScalaDaysSF2015/T4_Barnes_Typelevel_Prog.pdf
sealed trait IntType {
  type plus[That <: IntType] <: IntType
}

sealed trait Int0 extends IntType {
  override type plus[That <: IntType] = That
}

sealed trait IntN[Prev <: IntType] extends IntType {
  override type plus[That <: IntType] = IntN[Prev#plus[That]]
}

sealed trait IntListType[Size <: IntType] {
  def ::(head: Int): IntListType[IntN[Size]] = IntList(head,this)

  // def +(that: IntListType[Size]): IntListType[Size]

  def ++[ThatSize <: IntType](that: IntListType[ThatSize]): IntListType[Size#plus[ThatSize]]
}

case object IntNil extends IntListType[Int0] {
  // override def +(that: IntListType[IntType]): IntListType[IntType] = that

  override def ++[ThatSize <: IntType](that: IntListType[ThatSize]) = that
}

case class IntList[TailSize <: IntType] (head: Int, tail: IntListType[TailSize]) extends IntListType[IntN[TailSize]] {
  // override def +(that: IntListType[IntN[TailSize]]) = that match {
  //   case IntList(h, t) => (head + h) :: (tail + t)
  // }

  override def ++[ThatSize <: IntType](that: IntListType[ThatSize]) = IntList(head, tail ++ that)
}




