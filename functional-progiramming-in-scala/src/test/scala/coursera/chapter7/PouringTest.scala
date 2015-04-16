package coursera.chapter7

import org.scalatest._

class PouringTest extends FlatSpec with Matchers {

  "test" should "" in {
    val problem = new Pouring(Vector(4, 7, 8, 9))

    println(problem.solutions(6))
  }

}
