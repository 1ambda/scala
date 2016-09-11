package lambda

import language.experimental.macros
import reflect.macros.Context
import scala.collection.immutable.StringLike

object OctalConverter {
  implicit class OctalContext(val sc: StringContext) {
    def o(): Int = macro OctalConverter.oImpl
  }

  def oImpl(c: Context)(): c.Expr[Int] = {
    import c.universe._

    val Apply (
          _, List (
              Apply (
                _, List (Literal (Constant (orig : String)))))) =
      c.prefix.tree

    println(showRaw(c.prefix.tree))

    val OctalPattern = "[0-7]+".r

    orig match {
      case OctalPattern() =>
        c.Expr[Int] (Literal (Constant (Integer.parseInt(orig, 8))))
      case _ =>
        c.error(c.enclosingPosition, "Must only contain 0-7 characters")
        c.Expr[Int] (Literal (Constant (0)))
    }
  }
}
