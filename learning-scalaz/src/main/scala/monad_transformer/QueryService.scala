package monad_transformer

import monad_transformer.QueryService.TransactionState

import scalaz._, Scalaz._

trait Model
trait Query
trait QueryResult
trait Connection {
  def close: Unit
}
trait Transaction {
  def getConnection: Connection
  def commit: Unit = getConnection.close
  def rollback: Unit = getConnection.close
}

object QueryService {
  type TransactionState[A] = State[Transaction, A]
  type EitherStringT[F[_], A] = EitherT[F, String, A]
  type Transactional[A] = EitherStringT[TransactionState, A]

  def runQuery1(s: String, model: Model): Transactional[QueryResult] = for {
    query <- EitherT(parseQuery(s).point[TransactionState])
    result <- EitherT(performQuery(query, model).point[TransactionState])
  } yield result

  def runQuery(s: String, model: Model): Transactional[QueryResult] = for {
    query <- Transactional(parseQuery(s))
    result <- Transactional(performQuery(query, model))
    _ <- (modify { t: Transaction => t }).liftM[EitherStringT]
  } yield result

  def parseQuery(s: String): String \/ Query = ???
  def performQuery(q: Query, m: Model): String \/ QueryResult = ???
}

object Transactional {
  import QueryService._
  def apply[A](e: String \/ A): Transactional[A] = liftE(e)
  def liftE[A](e: String \/ A) = EitherT(e.point[TransactionState])
}
