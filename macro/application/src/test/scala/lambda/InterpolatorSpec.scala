package lambda

import lambda.DebugMacros._
import OctalConverter._

class InterpolatorSpec extends TestSuite {

  ignore("debug") {
    val x = 11
    debug(x)
    debug2(x)
  }
  test("o (OctalContext)") {
    println(
      o"""43""")
  }
}
