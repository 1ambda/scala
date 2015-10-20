package readerwriterstate

import java.lang.Thread

import org.scalatest._
import readerwriterstate.{Thread, Waiting}
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
      merged
    }

    val r = merge("200", "SELECT FROM PRIVATE_FOR_100", "SELECT FROM PRIVATE_FOR_100")
    r shouldBe None
  }

  // https://github.com/scalaz/scalaz/blob/series/7.2.x/example/src/main/scala/scalaz/example/WriterUsage.scala
  test("Writer usage2") {
    import readerwriterstate.Process._

    val (written, process) = createNewProcess.run

    process.threads.length shouldBe 1
    process.threads.head.name shouldBe "main"

    /* map lets you map over the value side */
    val ts: Logger[List[Thread]] = createNewProcess.map(p => p.threads)
    ts.value.length shouldBe 1

    /* with mapWritten you can map over the written side */
    val edited: Vector[String] = createNewProcess.mapWritten(_.map { log => "[LOG]" + log }).written
    // println(edited.mkString("\n"))

    /* with mapValue, you can map over both sides */
    createNewProcess.mapValue { case (log, p) =>
      (log :+ "Add an IO thread",
       p.copy(threads = Thread(genRandomID, "IO-1", Waiting) :: p.threads))
    }

    // `:++>` `:++>>`, `<++:`, `<<++:`
    createNewProcess :++> Vector("add some log")
    val emptyWithLog = createEmptyProcess :++>> { process =>
      Vector(s"${process.pid} is an empty process")
    }

    // println(emptyWithLog.written)

    // Writer is an applicative

    val emptyProcesses: Logger[List[readerwriterstate.Process]] =
      (createEmptyProcess |@| createEmptyProcess) { List(_) |+| List(_) }

    val ps = emptyProcesses.value
    ps.length shouldBe 2
  }
}
