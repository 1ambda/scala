package coursera.chapter5

object HigherOrderFunction {

  def squareList1(xs: List[Int]): List[Int] = xs match {
    case Nil => xs
    case x :: xs => x * x :: squareList1(xs)
  }

  def squareList2(xs: List[Int]): List[Int] = 
    xs map(x => x * x)

  def pack[T](xs: List[T]): List[List[T]] = xs match {
    case Nil => Nil
    case x :: xs1 => 
      xs.takeWhile(y => y == x) :: pack(xs.dropWhile(y => y == x))
  }

  def encode[T](xs: List[T]): List[(T, Int)] = pack(xs) map (ys => (ys.head, ys.length))
  // def encode1[T](xs: List[T]): List[(T, Int)] = pack(xs) match {
  //   case Nil => Nil
  //   case x :: xs1 => (x.head, x.length) :: encode(xs1)
  // }
}
