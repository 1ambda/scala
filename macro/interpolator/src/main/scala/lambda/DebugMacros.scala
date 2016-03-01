package lambda

import language.experimental.macros
import reflect.macros.Context


/** ref - http://www.warski.org/blog/2012/12/starting-with-scala-macros-a-short-tutorial/ */
object DebugMacros {
  def hello(): Unit = macro hello_impl
  def hello_impl(c: Context)(): c.Expr[Unit] = {
    import c.universe._
    reify { println("Hello World") }
  }

  def printParam(param: Any): Unit = macro printParam_impl
  def printParam_impl(c: Context)(param: c.Expr[Any]): c.Expr[Unit] = {
    import c.universe._

    reify { println(param.splice) }
  }

  def debug(param: Any): Unit = macro debug_impl
  def debug_impl(c: Context)(param: c.Expr[Any]): c.Expr[Unit] = {
    import c.universe._

    val paramRep = show(param.tree)
    val paramRepTree = Literal(Constant(paramRep))
    val paramRepExpr = c.Expr[String](paramRepTree)

    reify { println(paramRepExpr.splice + " = " + param.splice) }
  }
}
