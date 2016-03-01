package lambda

import lambda.DebugMacros._

class InterpolatorSpec extends TestSuite {

  val y = 10
  test("debug 1") {

    val z = 20

    val x = 11
    debug(x)
    debug(x + y)
    debug(x + z)
  }
}
