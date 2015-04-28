package calculator

sealed abstract class Expr
final case class Literal(v: Double) extends Expr
final case class Ref(name: String) extends Expr
final case class Plus(a: Expr, b: Expr) extends Expr
final case class Minus(a: Expr, b: Expr) extends Expr
final case class Times(a: Expr, b: Expr) extends Expr
final case class Divide(a: Expr, b: Expr) extends Expr

object Calculator {

  val validRefs: Set[String] = "abcdefghij".sliding(1).toSet

  def computeValues(
      namedExpressions: Map[String, Signal[Expr]]): Map[String, Signal[Double]] = {
    ???
  }

  def eval(expr: Expr, refs: Map[String, Signal[Expr]]): Double = {
    expr match {
      case Literal(value) => value
      case Ref(variable) =>
        val e = getReferenceExpr(variable, refs)
        eval(e, refs)
      case Plus(l, r) => eval(l, refs) + eval(r, refs)
      case Minus(l, r) => eval(l, refs) - eval(r, refs)
      case Times(l, r) => eval(l, refs) * eval(r, refs)
      case Divide(l, r) => eval(l, refs) / eval(r, refs)
    }
  }

  /** Get the Expr for a referenced variables.
   *  If the variable is not known, returns a literal NaN.
   */
  def getReferenceExpr(name: String, references: Map[String, Signal[Expr]]) = {
    references.get(name).fold[Expr] {
      Literal(Double.NaN)
    } {
      exprSignal => exprSignal()
    }
  }

}
