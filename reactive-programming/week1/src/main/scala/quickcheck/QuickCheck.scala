package quickcheck

import common._

import org.scalacheck._
import Arbitrary._
import Gen._
import Prop._

abstract class QuickCheckHeap extends Properties("Heap") with IntHeap {

  property("min1") = forAll { a: Int =>
    val h = insert(a, empty)
    findMin(h) == a
  }

  // example property
  property("empty") = forAll { a: Int =>
    val h = empty
    isEmpty(h) == true 
  }

  // example generator
  // Gen.const return a generator which always generates the given value
  // scalacheck Impl: https://github.com/rickynils/scalacheck/blob/master/src/main/scala/org/scalacheck/Gen.scala#L305 
  lazy val genMap: Gen[Map[Int, Int]] = for {
    k <- arbitrary[Int]
    v <- arbitrary[Int]
    m <- oneOf(const(Map.empty[Int, Int]), genMap)
  } yield m.updated(k, v)

  // example property using genMap
  property("map1") = forAll { m: Map[Int, Int] =>
    val k = 3;
    val v = 5;
    val m2 = m.updated(k, v)
    m2(k) == v
  }

  // generator for IntHeap
  lazy val genHeap: Gen[H] = for {
    element <- arbitrary[Int]
    heap <- oneOf(const(empty), genHeap)
  } yield insert(element, heap)

  // to use genHeap in forAll expression, we need Arbitrary[H]
  implicit lazy val arbHeap: Arbitrary[H] = Arbitrary(genHeap)

  lazy val genNonEmptyHeap: Gen[H] = for {
    h <- arbitrary[H]
    x <- arbitrary[Int]
  } yield if (isEmpty(h)) insert(x, h) else h

  // property for genNonEmptyHeap
  property("genNonEmptyHeap1") = forAll(genNonEmptyHeap) { (h: H) =>
    isEmpty(h) == false 
  }

  property("Bogus1") = forAll { _: Unit =>
    val h1 = insert(1, empty)
    val h2 = insert(2, h1)
    val h3 = insert(3, h2)
    findMin(h3) == 1 
  }

  property("Bogus2") = forAll(genNonEmptyHeap, arbitrary[Int]) { (h: H, x: Int) =>
    val heapMin = findMin(h)
    val min = if (heapMin < x) heapMin else x

    val h2 = insert(min, h)
    findMin(h2) == min
  }

  property("Bogus3") = forAll { (x: Int, y: Int) =>

    val min = if (x < y) x else y
    val max = if (x > y) x else y

    val h1 = insert(x, empty)
    val h2 = insert(y, h1)
    val h3 = deleteMin(h2)

    findMin(h3) == max
  }

  // helper for Bogus4
  def isSorted(xs: List[Int]) = (xs, xs.tail).zipped.forall(_ <= _)
  def removeAll(heap: H): List[Int] =
    if (isEmpty(heap)) Nil
    else {
      val min = findMin(heap)
      val h = deleteMin(heap)
      min :: removeAll(h)
    }

  def insertAll(xs: List[Int], heap: H): H = xs match {
    case Nil => empty
    case y :: ys =>
      insert(y, insertAll(ys, heap))
  }

  // property for finding bug in Bogus4
  property("Bogus4") = forAll { (xs: List[Int]) =>
    val h = insertAll(xs, empty)
    val ys = removeAll(h)
    xs.sorted == ys
  }

  property("Bogus5") = forAll(genNonEmptyHeap, genNonEmptyHeap) { (h1: H, h2: H) =>
    val h1Min = findMin(h1)
    val h2Min = findMin(h2)
    val min = if (h1Min < h2Min) h1Min else h2Min

    val h3 = meld(h1, h2)
    findMin(h3) == min
  }
}
