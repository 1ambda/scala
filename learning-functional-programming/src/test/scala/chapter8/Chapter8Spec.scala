package chapter8


import java.util.concurrent.{Executors, ExecutorService}

import org.scalatest.{Matchers, FunSuite}
import util.Par

class Chapter8Spec extends FunSuite with Matchers {

  test("max") {
    val smallInt = Gen.choose(-10, 10)
    val maxProp = Prop.forAll(Gen.listOf(smallInt)) { ns =>
      if (ns.size == 0)
        true
      else {
        val max = ns.max
        !ns.exists(_ > max)
      }
    }

    Prop.run(maxProp, 1000, 1000)
  }

  test("max2") {
    val smallInt = Gen.choose(-10, 10)
    val maxProp = Prop.forAll(Gen.listOf1(smallInt)) { ns =>
      val max = ns.max
      !ns.exists(_ > max)
    }

    Prop.run(maxProp, 1000, 1000)
  }

  test("sorted") {
    val smallInt = Gen.choose(-10, 10)
    val sortedProp = Prop.forAll(Gen.listOf(smallInt)) { ns =>
      val sorted = ns.sorted

      if (sorted.isEmpty || sorted.size == 1) true
      else {
        !sorted.zip(sorted.tail).exists { case (a, b) => a > b }
      }
    }

    Prop.run(sortedProp)
  }

  test("par unit test1") {
    val es: ExecutorService = Executors.newCachedThreadPool
    val p1 = Prop.forAll(Gen.unit(Par)) { i =>
      Par.map(Par.lazyUnit(1))(_ + 1)(es).get == Par.unit(2)(es).get
    }

    Prop.run(p1)
  }

  test("par unit test2") {
    val es: ExecutorService = Executors.newCachedThreadPool
    val p = Prop.check {
      Par.map(Par.lazyUnit(1))(_ + 1)(es).get == Par.unit(2)(es).get
    }

    Prop.run(p)
  }

  test("par unit test3") {
    val es: ExecutorService = Executors.newCachedThreadPool
    val p = Prop.check {
      Par.equal(
        Par.map(Par.unit(1))(_ + 1),
        Par.unit(2)
      )(es).get
    }

    Prop.run(p)
  }

  test("par unit test4") {
    val es: ExecutorService = Executors.newCachedThreadPool
    val p = Prop.checkPar {
      Par.equal(
        Par.map(Par.unit(1))(_ + 1),
        Par.unit(2)
      )
    }

    Prop.run(p)
  }

  test("par unit test5") {
    val es: ExecutorService = Executors.newCachedThreadPool
    val pint = Gen.choose(0, 10) map (Par.unit _)
    val p = Prop.forAllPar(pint) {n =>
      Par.equal(
        Par.map(n)(y => y),
        n
      )
    }

    Prop.run(p)
  }
}
