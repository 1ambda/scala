package free

import java.util.concurrent.atomic.AtomicInteger

import org.scalatest.{Matchers, FunSuite}

class JDBCSpec extends FunSuite with Matchers {

  test("JDBC.get* functions") {
    val rs = createDummyResultSet
  }

  def createDummyResultSet: ResultSet = new ResultSet {
    val counter = new AtomicInteger(0)
    override def next: Boolean = ???

    override def close: Unit = ???

    override def getInt(index: Int): Int = ???

    override def getString(index: Int): String = ???
  }
}
