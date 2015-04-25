package lecture

import org.junit.runner.RunWith
import org.scalatest.{ShouldMatchers, FunSuite}
import org.scalatest.junit.JUnitRunner

import scala.language.reflectiveCalls


@RunWith(classOf[JUnitRunner])
class LoopsTest extends FunSuite with ShouldMatchers {

  import Loops._

  test("WHILE test") {
    var i = 0
    WHILE(i <= 3) { i += 1 }
    assert(i == 4)
  }

  test("REPEAT UNTIL test") {
    var i = 0

    REPEAT {
      i += 1
    } UNTIL (i > 3)

    assert(i == 4)
  }

  test("for loop") {
    for (i <- 1 until 3) println(i)
    (1 until 3) foreach println
  }
}

object Loops {
  def WHILE(condition: => Boolean)(command: => Unit): Unit = {
    if (condition) {
      command
      WHILE(condition)(command)
    } else ()
  }

  def REPEAT(command: => Unit) =  new {
    def UNTIL(condition: => Boolean): Unit = {
      command

      if (condition) ()
      else UNTIL(condition)
    }
  }

}
