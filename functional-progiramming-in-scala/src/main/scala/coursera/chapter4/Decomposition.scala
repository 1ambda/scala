package coursera.chapter4

object Decomposition {
  def show(e: Expr): String = {
    e match {
      case Number(n) => n.toString
      case Var(x) => x
      case Sum(e1, e2) => show(e1) + " + " + show(e2)
      case Prod(Sum(e1, e2), r) => "(" + show(Sum(e1, e2)) + ")" + " * " + show(r)
      case Prod(l, Sum(e1, e2)) => show(l) + " * " + "(" + show(Sum(e1, e2)) + ")"
      case Prod(e1, e2) => show(e1) + " * " + show(e2)
    }
  }
}

trait Expr {
  def eval: Int = {
    this match {
      case Number(n) => n
      case Sum(e1, e2) => e1.eval + e2.eval
    }
  }
}

case class Number(n: Int) extends Expr
case class Var(x: String) extends Expr
case class Sum(l: Expr, r: Expr) extends Expr
case class Prod(l: Expr, r: Expr) extends Expr

// trait Expr {
//   def isNumber: Boolean
//   def isSum: Boolean
//   def numValue: Int
//   def leftOp: Expr
//   def rightOp: Expr
// }

// class Number(n: Int) extends Expr {
//   def isNumber = true
//   def isSum = false
//   def numValue = n
//   def leftOp = throw new Error("Number.leftOp")
//   def rightOp = throw new Error("Number.righOp")
// }

// class Sum(l: Expr, r: Expr) extends Expr {
//   def isNumber = false
//   def isSum = true
//   def numValue = throw new Error("Sum.numValue")
//   def leftOp = l
//   def rightOp = r
// }
