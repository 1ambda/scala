package reader_writer

import org.scalatest._
import scalaz._, Scalaz._

class WriterSpec extends FunSuite with Matchers {

  // ref - http://eed3si9n.com/learning-scalaz/Writer.html
  test("WriterOps") {
    val w1 = 10.set("example")
    w1.run shouldBe ("example", 10)
  }

  test("Writer usage") {
    def executeQuery(id: String, query: String): Writer[List[String], Option[Int]] =
      if (id.startsWith("100")) 10.some.set(List(s"$id executed $query"))
      else none[Int].set(List(s"Invalid ID: $id"))

    def merge(id: String, query1: String, query2: String): Option[Int] = {
      val w = for {
        o1 <- executeQuery(id, query1)
        o2 <- executeQuery(id, query2)
      } yield (o1 |+| o2) /* mappend for Option */

      val (logs, merged) = w.run
//      println(logs)
      merged
    }

    val r = merge("200", "SELECT FROM PRIVATE_FOR_100", "SELECT FROM PRIVATE_FOR_100")
    r shouldBe None
  }

  test("Writer usage2") {
    import Process._

    val (log, process) = createNewProcess.run
//    println(log)
//    println(process)
    process.threads.length shouldBe 1
    process.threads.head.name shouldBe "main"
  }
}
