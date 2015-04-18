package lecture

// week1

import org.scalatest._
import org.scalacheck.Prop.forAll

import java.util.Random

trait Generator[+T] {
  self => // alias for this
  def generate: T

  def map[S](f: T => S): Generator[S] = new Generator[S] {
    def generate = f(self.generate)
  }

  // 
  def flatMap[S](f: T => Generator[S]): Generator[S] = new Generator[S] {
    def generate = f(self.generate).generate
  }
}

object Generator {

  /*
  'new Generator' is a boilerplate

  val booleans = new Generator[Boolean] {
    def generate = integers.generate > 0
  }

  val paris = new Generator[(Int, Int)] {
    def generate = (integers.generate, integers.generate)
  }
   */

  val integers = new Generator[Int] {
    val rand = new Random
    def generate = rand.nextInt
  }

  // val booleans = integers map (x => x > 0)
  val booleans = for (x <- integers) yield x > 0

  // def pairs(t, u) = t flatMap(x => u map (y => (x, y)))
  def pairs[T, U](t: Generator[T], u: Generator[U]): Generator[(T, U)] = for {
    x <- t
    y <- u
  } yield (x, y)

  def single[T](x: T): Generator[T] = new Generator[T] {
    def generate = x
  }

  def choose(low: Int, high: Int): Generator[Int] =
    for (x <- integers) yield low + x % (high - low)

  def oneOf[T](xs: T*): Generator[T] =
    for (idx <- choose(0, xs.length)) yield xs(idx)

  def lists: Generator[List[Int]] = for {
    isEmpty <- booleans
    list <- if (isEmpty) emptyList else nonEmptyList
  } yield list

  def emptyList: Generator[List[Int]] = single(Nil)
  def nonEmptyList : Generator[List[Int]] = for {
    head <- integers
    tail <- lists
  } yield head :: tail
}

trait Tree
object Tree {
  import Generator._
  case class Inner(left: Tree, right: Tree) extends Tree
  case class Leaf(x: Int) extends Tree

  def inners: Generator[Tree] = for {
    l <- trees
    r <- trees
  } yield Inner(l, r)

  def leafs: Generator[Tree] = for (x <- integers) yield Leaf(x)

  def trees: Generator[Tree] = for {
    isLeaf <- booleans
    tree <- if (isLeaf) leafs else inners 
  } yield tree
}

class GeneratorTest extends FlatSpec with Matchers {
  import Generator._
  import Tree._

  // scalaCheck forAll
  // http://www.scalatest.org/user_guide/generator_driven_property_checks
  "single(x)" should "return a generator which always returns x" in  {
    val prop = forAll { (x: Int) => single(x).generate == x }
    prop.check
  }

  "choose(l, h)" should "return an integers between l and h" in  {
    val prop =
      forAll { (l: Int, h: Int) =>
        val x = choose(l, h).generate
        l <= x && x <= h
      }
    prop.check
  }

  "oneOf(xs)" should "return one of xs" in {
    val prop =
      forAll { (xs: List[Int]) =>
        val x = Generator.oneOf(xs: _*).generate
        xs contains x
      }

    prop.check
  }
}


