package chapter8

import org.scalatest.{Matchers, FunSuite}

class Chapter8Spec extends FunSuite with Matchers {

  test("max") {
    val smallInt = Gen.choose(-10, 10)
    val maxProp = Prop.forAll(Gen.listOf(smallInt)) { ns =>
      val max = ns.max
      !ns.exists(_ > max)
    }

    println(maxProp)
  }
}
