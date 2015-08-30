package chapter9

import java.util.regex.Pattern

import chapter8._
import Prop._

import scala.util.matching.Regex

trait Parser[+A] { self =>
  def map[B](f: A => B): Parser[B] = Parser.map(self)(f)
  def flatMap[B](f: A => Parser[B]): Parser[B] = Parser.flatMap(self)(f)
  def | [B>:A](other: Parser[B]): Parser[B] = Parser.or(self, other)
  def slice[A] = Parser.slice(self)
  def many[A] = Parser.many(self)
  def **[B](other: Parser[B]): Parser[(A, B)] = Parser.product(self, other)
  def <*(other: Parser[Any]): Parser[A] = Parser.skipR(self, other)
  def *>[B](other: Parser[B]): Parser[B] = Parser.skipL(self, other)
  def sep(separator: Parser[Any]) = Parser.sep(self, separator)
  def sep1(separator: Parser[Any]) = Parser.sep1(self, separator)
}

object Parser {
  def count[A](p: Parser[A]): Parser[Int] = p.many.map(_.size)
  def char(c: Char): Parser[Char] = string(c.toString).map(s => s.charAt(0))
  def string(s: String): Parser[String] = ???
  def run[A](p: Parser[A])(s: String): Either[ParseError, A] = ???

  implicit def strToParser(s: String): Parser[String] = ???
  def or[A](p1: Parser[A], p2: => Parser[A]): Parser[A] = ???
  def orTest(s1: String, s2: String): Parser[String] = "a" | "b"

  def slice[A](p: Parser[A]): Parser[String] = ???

  def numA: Parser[Int] = char('a').many.map(_.size)
  def numAB: Parser[Int] = (char('a') | char('b')).many.map(_.size)

  def succeed[A](a: A): Parser[A] = string("") map (_ => a)

  def map2UsingProduct[A, B, C](p1: Parser[A], p2: Parser[B])(f: (A, B) => C): Parser[C] =
    product(p1, p2).map(f.tupled)
  def productUsingMap2[A, B](p1: Parser[A], p2: Parser[B]): Parser[(A, B)] =
    map2(p1, p2)((a, b) => (a, b))

  def many1[A](p: Parser[A]): Parser[List[A]] =
    map2(p, p.many)(_ :: _)

  // many: or, map2, succeed
  def many[A](p: Parser[A]): Parser[List[A]] =
    map2(p, p.many)(_ :: _) | succeed(List())

  def listOfN[A](n: Int, p: Parser[A]): Parser[List[A]] =
    if (n <= 0) succeed(List())
    else map2(p, listOfN(n -1, p))(_ :: _)

  def flatMap[A, B](p: Parser[A])(f: A => Parser[B]): Parser[B] = ???

  implicit def regex(r: Regex): Parser[String] = ???

  def regexTest: Parser[Int] = for {
    digit <- "[0-9]+".r
    n = digit.toInt
    _ <- listOfN(n, char('a'))
  } yield n

  def map[A, B](p: Parser[A])(f: A => B): Parser[B] =
    p.flatMap(a => succeed(f(a)))

  def product[A, B](p1: Parser[A], p2: => Parser[B]): Parser[(A, B)] =
    p1.flatMap(a => p2.map(b => (a, b)))

  def map2[A, B, C](p1: Parser[A], p2: => Parser[B])(f: (A, B) => C): Parser[C] =
    p1.flatMap(a => p2.map(b => f(a, b)))

  def skipL[B](p1: Parser[Any], p2: => Parser[B]): Parser[B] =
    map2(p1.slice, p2)((_, b) => b)

  def skipR[A](p1: => Parser[A], p2: Parser[Any]): Parser[A] =
    map2(p1, p2.slice)((a, _) => a)

  def surround[A](start: Parser[Any], end: Parser[Any])(p: Parser[A]): Parser[A] =
    start *> p <* end

  def sep[A](p: Parser[A], separator: Parser[Any]): Parser[List[A]] =
    sep1(p, separator) | succeed(List())

  def sep1[A](p: Parser[A], separator: Parser[Any]): Parser[List[A]] =
    map2(p, many(separator *> p))(_ :: _)

  def eof: Parser[String] = regex("\\z".r)

  def root[A](p: Parser[A]): Parser[A] = p <* eof

  def closed(closer: String): Parser[String] = (".*?" + Pattern.quote(closer)).r
  def enclosed(pattern: String) = string(pattern) *> closed(pattern) map (_.dropRight(1))
  def quoted: Parser[String] = enclosed("\"")
  def escapedQuote: Parser[String] = enclosed("\\\"")

  def letter: Parser[Char] = regex("[a-zA-Z]".r) map(_.charAt(0))
  def whitespace: Parser[String] = "\\s*".r
  def digits: Parser[String] = "\\d+".r
  def boolean: Parser[Boolean] = regex("[(true)(false)]".r) map (_.toBoolean)
  def string: Parser[String] = letter.many map (cs => cs.mkString)
  def double: Parser[Double] = regex("[-+]?([0-9]*\\.[0-9]+|[0-9]+)".r) map (_.toDouble)


  def label[A](message: String)(p: Parser[A]): Parser[A] = ???
  def scope[A](message: String)(p: Parser[A]): Parser[A] = ???

}

object Laws {
  import Parser._

  def equal[A](p1: Parser[A], p2: Parser[A])(in: Gen[String]): Prop =
    forAll(in) { s => run(p1)(s) == run(p2)(s) }

  def mapLaw[A](p: Parser[A])(in: Gen[String]): Prop
    = equal(p.map(x => x), p)(in)
}



