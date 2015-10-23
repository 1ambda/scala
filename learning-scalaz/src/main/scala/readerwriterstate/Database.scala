package readerwriterstate

import java.util.UUID

import scalaz._, Scalaz._

import Database._

import com.github.nscala_time.time.Imports._

// ref - http://underscore.io/blog/posts/2014/07/27/readerwriterstate.html


case class ResultSet()

case class Connection(id: String,
                      actions: List[PostCommitAction] = Nil) {

  def commit = {}
  def rollback = {}
  def getResultSet(query: String): ResultSet = ResultSet()
  def executeQuery(query: String): Unit = {}
}

case class PostCommitAction(id: String, action: Action)
case class DatabaseConfig(operationTimeoutMillis: Long)

class OperationTimeoutException private(ex: RuntimeException) extends RuntimeException(ex) {
  def this(message:String) = this(new RuntimeException(message))
  def this(message:String, throwable: Throwable) = this(new RuntimeException(message, throwable))
}

object OperationTimeoutException {
  def apply(message:String) = new OperationTimeoutException(message)
  def apply(message:String, throwable: Throwable) = new OperationTimeoutException(message, throwable)
}

/**
 *  object ReaderWriterState extends ReaderWriterStateTInstances with ReaderWriterStateTFunctions {
 *    def apply[R, W, S, A](f: (R, S) => (W, A, S)): ReaderWriterState[R, W, S, A] =
 *      IndexedReaderWriterStateT[Id, R, W, S, S, A] { (r: R, s: S) => f(r, s) }
 *  }
 *
 *  sealed abstract class IndexedReaderWriterStateT[F[_], -R, W, -S1, S2, A] { self =>
 *    def run(r: R, s: S1): F[(W, A, S2)]
 *    ...
 *  }
 */

object Database {
  type Key = String
  type Action = () => Unit
  type Task[A] = ReaderWriterState[DatabaseConfig, Vector[String] /* log */, Connection, A]

  object Implicit {
    implicit def defaultConnection: Connection = Connection(genRandomUUID)
    implicit def defaultConfig = DatabaseConfig(500)
  }

  private def genRandomUUID: String = UUID.randomUUID().toString

  private def execute[A](f: => A, conf: DatabaseConfig): A = {
    val start = DateTime.now

    val a = f

    val end = DateTime.now

    val time: Long = (start to end).millis

    if (time > conf.operationTimeoutMillis)
      throw OperationTimeoutException(s"Operation timeout: $time millis")

    a
  }

  def createTask[A](f: Connection => A): Task[A] =
    ReaderWriterState { (conf, conn) =>
      val a = execute(f(conn), conf)
      (Vector(s"Task was created with connection[${conn.id}]"), a, conn)
    }

  def addPostCommitAction(action: Action): Task[Unit] =
    ReaderWriterState { (conf, conn: Connection) =>

      val postCommitAction = PostCommitAction(genRandomUUID, action)
      (Vector(s"Add PostCommitAction(${postCommitAction.id})"),
        Unit,
        conn.copy(actions = conn.actions :+ postCommitAction))

    }

  def run[A](task: Task[A])
            (implicit defaultConf: DatabaseConfig, defaultConn: Connection): Option[A] = {

    \/.fromTryCatchThrowable[(Vector[String], A, Connection), Throwable](
      task.run(defaultConf, defaultConn)
    ) match {
      case -\/(t) =>
        println(s"Operation failed due to ${t.getMessage}") /* logging */
        none[A]

      case \/-((log: Vector[String], a: A, conn: Connection)) =>
        conn.commit /* close connection */

        log.foreach { text => println(s"[LOG] $text")} /* logging */

        /* run post commit actions */
        conn.actions foreach { _.action() }

        a.some
    }
  }
}


