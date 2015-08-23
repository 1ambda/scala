package chapter9

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
}

object Parser {
  type FailureMessage = String

  def count[A](p: Parser[A]): Parser[Int] = p.many.map(_.size)
  def char(c: Char): Parser[Char] = string(c.toString).map(s => s.charAt(0))
  def string(s: String): Parser[String] = ???
  def run[A](p: Parser[A])(s: String): Either[FailureMessage, A] = ???

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

  // many: or, map2, successed
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
}

object ParserLaws {
  import Parser._

  def equal[A](p1: Parser[A], p2: Parser[A])(in: Gen[String]): Prop =
    forAll(in) { s => run(p1)(s) == run(p2)(s) }

  def mapLaw[A](p: Parser[A])(in: Gen[String]): Prop
    = equal(p.map(x => x), p)(in)
}



