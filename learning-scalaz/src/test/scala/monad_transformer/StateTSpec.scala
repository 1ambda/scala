package monad_transformer

import util.TestUtils
import scalaz._, Scalaz._

class StateTSpec extends TestUtils {

  "replicateM(10)" in {

    val getAndIncrement: State[Int, Int] = State { s => (s + 1, s) }
    getAndIncrement.replicateM(10).run(0) shouldBe (10, (0 until 10).toList)
  }

"replicateM(1000)" in {

  import scalaz.Free._

  val getAndIncrement: State[Int, Int] = State { s => (s + 1, s) }
  getAndIncrement.lift[Trampoline].replicateM(1000).run(0).run shouldBe (1000, (0 until 1000).toList)
}
}
