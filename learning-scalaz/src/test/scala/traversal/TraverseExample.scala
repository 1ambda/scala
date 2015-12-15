package traversal

import util.WordTestSuite

import scalaz._, Scalaz._
import scala.concurrent._
import scala.util.{Success, Failure}

/* ref - http://stackoverflow.com/questions/26602611/how-to-understand-traverse-traverseu-and-traversem */
class TraverseExample extends WordTestSuite {
  import ExecutionContext.Implicits.global

  "sequence" in {
    List(1.some, 2.some).sequence shouldBe Some(List(1, 2))
    val fxs: Future[List[Int]] =
      List(Future.successful(1), Future.successful(2)).sequence
  }

  /**
   *
   */
  "traverse" in {
    def fetchPost(id: Int): Future[String] =
      Future.successful("body" + id.toString)

    val result1: Future[List[String]] =
      List(1, 2).traverse[Future, String](fetchPost)

    val result2: Future[List[String]] =
      List(1, 2).traverseU(fetchPost)
  }
}
