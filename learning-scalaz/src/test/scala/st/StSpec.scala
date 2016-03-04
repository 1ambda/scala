package st

import org.scalatest.{Matchers, FunSuite}

import scalaz._

/** https://github.com/scalaz/scalaz/blob/series/7.3.x/example/src/main/scala/scalaz/example/STUsage.scala */
class StSpec extends FunSuite with Matchers {

  test("ST usage 1") {
    import effect._
    import ST._

    def e1[A] = for {
      r <- newVar[A](0)
      x <- r.mod(_ + 1)
    } yield x

    def e2[A] = e1[A].flatMap(_.read)


  }
}
