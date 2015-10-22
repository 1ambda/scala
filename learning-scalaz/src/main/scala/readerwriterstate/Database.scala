package readerwriterstate

import java.util.UUID

import scalaz._, Scalaz._

import Database._

import com.github.nscala_time.time.Imports._

// ref - http://underscore.io/blog/posts/2014/07/27/readerwriterstate.html



case class Connection(id: String,
                      actions: List[PostCommitAction] = Nil) {

  def getLock = {}
  def releaseLock = {}
  def commit = {}
  def rollback = {}
  def executeRegisteredActions = actions foreach { _ }
}

case class PostCommitAction(id: String, action: Action)
case class DatabaseConfig(operationTimeout: Long)

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

  def genRandomUUID: String = UUID.randomUUID().toString

  def perform[A](f: => A): (A, Long) = {
    val start = DateTime.now

    val a = f

    val end = DateTime.now

    (a, (end to start).millis)
  }

  def createTask[A](f: Connection => A): Task[A] =
    ReaderWriterState { (conf, conn) =>

      val (a, time) = perform(f(conn))

      if (time > conf.operationTimeout) {
        conn.rollback
        throw OperationTimeoutException(s"Operation timeout: $time mills")
      } else
        (Vector(s"Task was created with connection[${conn.id}]"), a, conn)
    }

  def addPostCommitAction(action: PostCommitAction): Task[Unit] =
    ReaderWriterState { (conf, conn: Connection) =>
      (Vector(s"Add PostCommitAction(${action.id})"), Unit, conn.copy(actions = conn.actions :+ action))
    }

  def run[A](task: Task[A])
            (implicit defaultConf: DatabaseConfig, defaultConn: Connection): String \/ A = {

    val e: Throwable \/ (Vector[String], A, Connection) =
      \/.fromTryCatchThrowable(task.run(defaultConf, defaultConn))

    e match {
      case Left(t: Throwable) =>
        println(s"Operatino failed due to ${t.getMessage}")
      case Right()
    }


  }





}


