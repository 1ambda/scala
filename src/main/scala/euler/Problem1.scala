package euler.Problem1

object Problem1 extends App {
  def getMultiples(acc: Int)(max: Int)(n: Int): Int = {
    if (max == 0) acc
    else if (max % n == 0) getMultiples(acc + max)(max - 1)(n)
    else getMultiples(acc)(max - 1)(n)
  }

  val until_1000 = getMultiples(0)(999) _
  println(until_1000(3) + until_1000(5) - until_1000(15))
}

