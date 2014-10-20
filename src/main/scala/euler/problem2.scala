package euler.Problem2

object Problem2 extends App {

  def fibo(n: Int): Int = n match {
    case 1 => 1
    case 2 => 2
    case _ => fibo(n-1) + fibo(n-2)
  }

  def sumEvenFibo(n: Int, acc: Int): Int = fibo(n) match {
    case i if i > 4000000 => acc
    case j if j % 2 != 0 => sumEvenFibo(n + 1, acc)
    case k => sumEvenFibo(n + 1, acc + k)
  }

  println(sumEvenFibo(1, 0))
}
