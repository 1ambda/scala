package monad_transformer

import scalaz._, Scalaz._

trait Model
trait Query
trait QueryResult
trait Transaction

object QueryService {
  type TransactionState[+A] = State[Transaction, A]
  type Transactional[+A] = EitherT[TransactionState, String, A]

  def runQuery(s: String, model: Model): Transactional[QueryResult] = for {
    query <- parseQuery(s)
    result <- performQuery(query, model)
  } yield result

  def parseQuery(s: String): Transactional[QueryResult] = ???
  def performQuery(q: Query, m: Model): Transactional[QueryResult] = ???
}
