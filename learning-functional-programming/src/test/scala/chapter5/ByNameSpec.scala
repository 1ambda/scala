package chapter5

import org.scalatest.{Matchers, FunSuite}

class ByNameSpec extends FunSuite with Matchers {
  import ByName._

  test("if2 test") {
    // if remove () => from the second arg, test will be fail
    if2(false, () => sys.error("fail"), () => 3)
  }

  test("if3 test") {
    if3(false, sys.error("fail"), 3)
  }

  test("twice test") {
    val value = twice(true, { println("42"); 42 })
    value should be (84)
  }

  test("lazy twice test") {
    val value = lazyTwice(true, { println("42"); 42 })
    value should be (84)
  }

}
