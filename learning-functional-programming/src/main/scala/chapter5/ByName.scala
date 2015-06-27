package chapter5

object ByName {
  def if2[A](cond: Boolean, onTrue: () => A, onFalse: () => A): A =
    if (cond) onTrue() else onFalse()

  def if3[A](cond: Boolean, onTrue: => A, onFalse: => A): A =
    if(cond) onTrue else onFalse

  def twice(b: Boolean, i: => Int) = if (b) i + i else 0
  def lazyTwice(b: Boolean, i: => Int) = {
    lazy val j = i
    if (b) j + j else 0
  }
}
