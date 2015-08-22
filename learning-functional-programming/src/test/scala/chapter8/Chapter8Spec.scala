package chapter8

import org.scalatest.{Matchers, FunSuite}

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
}
