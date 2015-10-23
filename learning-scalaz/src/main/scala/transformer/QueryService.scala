package transformer

import scalaz._, Scalaz._

trait Model
trait Query
trait QueryResult

trait Transaction {
  def closeConnection: Unit = {}
  def commit: Unit = closeConnection
  def rollback: Unit = closeConnection
}

object QueryService {
  type TransactionState[A] = State[Transaction, A]
  type EitherStringT[F[_], A] = EitherT[F, String, A]
  type Transactional[A] = EitherStringT[TransactionState, A]

  def parseQuery(s: String): String \/ Query = {
    println("parseQuery")
    if (!s.startsWith("SELECT")) {
      s"Invalid Query: $s".left[Query]
    }
    else (new Query {}).right[String]
  }

  def performQuery(q: Query, m: Model): String \/ QueryResult = {
    println("performQuery")
    new QueryResult {}.right
  }

  def runQuery(s: String, model: Model): Transactional[QueryResult] = for {
    query <- Transactional(parseQuery(s))
    result <- Transactional(performQuery(query, model))
    _ <- commit
  } yield result

  def commit: Transactional[Unit] =
    (modify { t: Transaction => t.commit; t }).liftM[EitherStringT]
  }

object Transactional {
  import QueryService._
  def apply[A](e: String \/ A): Transactional[A] = e match {
    case -\/(error) =>
      /* logging error and... */
      println("rollback")
      liftTS(State[Transaction, String \/ A] { t => t.rollback; (t, e) })
    case \/-(a) => liftE(e)
  }

  def liftE[A](e: String \/ A): Transactional[A] =
    EitherT(e.point[TransactionState])

  def liftTS[A](tse: TransactionState[String \/ A]): Transactional[A] =
    EitherT(tse)

}
