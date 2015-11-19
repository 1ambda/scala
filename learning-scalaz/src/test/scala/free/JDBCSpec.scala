package free

import java.util.concurrent.atomic.AtomicInteger

import org.scalatest.{Matchers, FunSuite}
import scalaz._, Scalaz._

class JDBCSpec extends FunSuite with Matchers {

  import JDBC._

  test("JDBC.getPerson") {
    val rs = createDummyResultSet(5)
    val r = JDBC.run(getPerson, rs).unsafePerformIO()

    r.age shouldBe 0
  }

  test("JDBC.getNextPerson") {
    val rs = createDummyResultSet(5)

    // def getNextPerson = next *> getPerson
    val r = JDBC.run(getNextPerson, rs).unsafePerformIO()

    r.age shouldBe 1
  }

  test("JDBC.getAllPeople") {
    val rs = createDummyResultSet(5)
    val r = JDBC.run(getAllPeople, rs).unsafePerformIO()

    r.length shouldBe 5
  }

  def createDummyResultSet(max: Int): ResultSet = new ResultSet {
    val counter = new AtomicInteger(0)
    val rand = new java.util.Random()

    override def next: Boolean =
      if (counter.getAndIncrement() < max) true
      else false

    override def close: Unit = {}

    override def getInt(index: Int): Int =
      counter.get()

    override def getString(index: Int): String =
      List.fill(5)(genRandomLetter).mkString

    private def genRandomLetter(): Char = {
      (rand.nextInt(27) + 64).toChar
    }
  }
}
