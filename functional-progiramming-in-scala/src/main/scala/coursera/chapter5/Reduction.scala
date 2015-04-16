package coursera.chapter5

object Redunction {

  def sum1(xs: List[Int]) = (0 :: xs) reduceLeft((x, y) => x + y)
  def product1(xs: List[Int]) = (1 :: xs) reduceLeft((x, y) => x * y)
  def sum2(xs: List[Int]) = (xs foldLeft 0)(_ + _)
  def product2(xs: List[Int]) = xs.foldLeft(1)(_ * _)
  def product3(xs: List[Int]) = (1 /: xs)(_ * _)

  def lengthFun[T](xs: List[T]): Int =
    (xs foldRight 0)((x, y) => y + 1)

  def mapFun[T, U](xs: List[T], f: T => U): List[U] =
    (xs foldRight List[U]())((x, ys) => f(x) :: ys)
}
