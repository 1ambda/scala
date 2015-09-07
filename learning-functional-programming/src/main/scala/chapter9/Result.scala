package chapter9

trait Result[+A] {
  def mapError(f: ParseError => ParseError): Result[A] = this match {
    case Failure(e) => Failure(f(e))
    case _ => this
  }
}
case class Success[+A](get: A, charsConsumed: Int) extends Result[A]
case class Failure(get: ParseError) extends Result[Nothing]
