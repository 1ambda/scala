package chapter9

case class ParseError(stack: List[(Location, String)])
