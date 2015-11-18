package readerwriterstate

import org.scalatest.{FunSuite, Matchers}

object Database2 {
  type Key = String

  def run[T](f: => T): T =
    try {
      startTransaction()
      val result = f
      commit()
      result
    } catch {
      case t => rollback(); throw t;
    }

  def startTransaction() = {}
  def commit() = {}
  def rollback() = {}
  def addPostCommit(f: () => Unit): Unit = {}
  def put[A](a: A): Unit = {}
  def find[A](key: Key): Option[A] = None
}


// ref: http://underscore.io/blog/posts/2014/07/27/readerwriterstate.html
class ReaderSpec2 extends FunSuite with Matchers {

  import Database2._

  test("side-effectful database example") {
    val result: Option[String] = Database2.run {
      put("stuff")
      addPostCommit(() => println("post action"))
      find("foo")
    }
  }

  test("functional database example using Reader") {
    import FunctionalDatabase._
    val task: Work[Option[String]] = for {
      _ <- FunctionalDatabase.put("foo", "Bar")
      found <- FunctionalDatabase.find[String]("foo")
    } yield found

    val result: Option[String] = FunctionalDatabase.run(task)
  }
}

object FunctionalDatabase {
  import scalaz.Reader

  type Key = String
  type Work[A] = Reader[Transaction, A]
  trait Transaction
  object CustomTransaction extends Transaction

  def run[T](work: Work[T]): T =
    try {
      startTransaction()
      val result = work.run(CustomTransaction)
      commit()
      result
    } catch { case t => rollback(); throw t }

  def startTransaction() = {}
  def commit() = {}
  def rollback() = {}

  def put[A](key: Key, a: A): Work[Unit] = Reader(Transaction => {})
  def find[A](key: Key): Work[Option[A]] = Reader(Transaction => None)
}


