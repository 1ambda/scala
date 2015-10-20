package readerwriterstate

import scalaz._, Scalaz._

// ref - http://underscore.io/blog/posts/2014/07/27/readerwriterstate.html

trait Transaction
case class JDBCTransaction() extends Transaction


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
  type PostCommitAction = () => Unit
  type DatabaseTask[A] = ReaderWriterState[Transaction, List[PostCommitAction], Unit, A]
  type Key = String

  def createTask[A](f: Transaction => A): DatabaseTask[A] =
    ReaderWriterState {
      (t, ignored) => (Nil, f(t), ()) // (r, s) => (w, a, s)
    }



}


