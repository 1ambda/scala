package coursera.chapter6

object Chapter6 {
  def isPrime(n: Int): Boolean = 2 to (n / 2) forall { d => n % d != 0}
  def getPrimePairs(n: Int): Seq[(Int, Int)] =
    1 until n flatMap { i => 1 until i map {j => (i, j)} } filter { case (i, j) => isPrime(i + j)}

  def getPrimePairs2(n: Int): Seq[(Int, Int)] =
    for {
      i <- 1 until n
      j <- 1 until i
      if isPrime(i + j)
    } yield (i, j)

  def scalaProduct(xs: List[Double], ys: List[Double]): Double =
    (for ((x, y) <- xs zip ys) yield (x * y)).sum

  def nQueens(n: Int): Set[List[Int]] = {

    def isSafe(col: Int, queens: List[Int]): Boolean = {
      val row = queens.length // where new queen will be placed
      val queensWithRow = (row - 1 to 0 by -1) zip queens
      queensWithRow forall {
        case (r, c) => col != c && math.abs(col - c) != row - r
      }
    }

    def placeQueens(k: Int): Set[List[Int]] =
      if (k == 0) Set(List())
      else
        for {
          queens <- placeQueens(k - 1)
          col <- 0 until n
          if isSafe(col, queens)
        } yield col :: queens

    placeQueens(n)
  }

  def showQueens(queens: List[Int]) = {
    val lines =
      for {
        col <- queens.reverse
      } yield Vector.fill(queens.length)("[ ]").updated(col, "[*]").mkString

    "\n\n" + (lines.mkString("\n"))
  }

  class Poly(val terms0: Map[Int, Double]) {
    def this(arg: (Int, Double)*) = this(arg.toMap)
    val terms = terms0 withDefaultValue 0.0
    //def + (other: Poly): Poly = new Poly(terms ++ (other.terms map adjust))
    def + (other:Poly): Poly = new Poly((other.terms foldLeft terms)(addTerm))
    def addTerm(ts: Map[Int, Double], t: (Int, Double)): Map[Int, Double] = {
      val (exp, coeff) = t
      ts + (exp -> (coeff + terms(exp)))
    }
    def adjust(term: (Int, Double)): (Int, Double) = {

      val (exp, coeff) = term
      (exp, coeff + terms(exp))
    }
    override def toString = {
      (for((exp, coeff) <- terms.toList.sorted.reverse) yield  coeff + "x^" + exp) mkString " + "
    }
  }
}
