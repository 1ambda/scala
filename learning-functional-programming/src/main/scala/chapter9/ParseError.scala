package chapter9

case class ParseError(stack: List[(Location, String)]) {
  def push(loc: Location, message: String): ParseError =
    copy(stack = (loc, message) :: stack)
}

