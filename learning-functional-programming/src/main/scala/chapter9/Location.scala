package chapter9

import chapter8._
import Prop._

case class Location(input: String, offset: Int = 0) {
    lazy val line = input.slice(0, offset + 1).count(_ == '\n') + 1
    lazy val column = input.slice(0, offset + 1).lastIndexOf('\n') match {
        case -1 => offset + 1
        case lineStart => offset - lineStart
    }

    def errorLocation(e: ParseError): Location = ???
    def errorMessage(e: ParseError): String = ???

    import Parser._
//    def labelLaw[A](p: Parser[A], inputs: SGen[String]): Prop =
//        forAll(inputs ** Gen.string) { case (input, message) =>
//            run(label(message)(p))(input) match {
//                case Left(e) => errorMessage(e) == message
//                case _ => true
//            }
//        }
}

