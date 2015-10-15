package monad_transformer

import util.TestUtils

import scalaz._, Scalaz._

class EitherTSpec extends TestUtils {
  import QueryService._

  "test" in {
    val tx: Transactional[Query] =
      EitherT.eitherT(parseQuery("qqq").point[TransactionState])
  }

}
