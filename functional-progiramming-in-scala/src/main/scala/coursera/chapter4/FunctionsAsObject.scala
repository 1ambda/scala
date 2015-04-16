package coursera.chapter4

object List {
  def apply() = Nil
  def apply(x: Int) = new Cons(x, Nil)
  def apply(x: Int, y: Int) = new Cons(x, new Cons(y, Nil))
}

trait List[+T] {
  def isEmpty: Boolean
  def head: T
  def tail: List[T]
  def prepend[U >: T](elem: U): List[U] = new Cons(elem, this)
}

class Cons[T](val head: T, val tail: List[T]) extends List[T] {
  def isEmpty = false
}

object Nil extends List[Nothing] {
  def isEmpty = true
  def head = throw new NoSuchElementException("Nil.head")
  def tail = throw new NoSuchElementException("Nil.tail")
}
