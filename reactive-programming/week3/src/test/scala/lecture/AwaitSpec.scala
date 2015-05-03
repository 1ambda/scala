package lecture

import org.junit.runner.RunWith
import org.scalatest.concurrent.AsyncAssertions
import org.scalatest.time._
import org.scalatest.{ParallelTestExecution, Matchers, FunSuite}
import org.scalatest.junit.JUnitRunner

import scala.collection.mutable.ListBuffer
import scala.concurrent._
import scala.async.Async._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Random}

@RunWith(classOf[JUnitRunner])
class AwaitSpec extends FunSuite with Matchers with ParallelTestExecution with AsyncAssertions {

  val limit = timeout(Span(3, Seconds))

  def randomSleep = Thread.sleep(Random.nextInt(200))
  def randomFutures: List[Future[String]] = (1 to 10).toList.map(x => Future {
    randomSleep
    x.toString
  })

  def awaitSequence[T](fs: List[Future[T]]): Future[List[T]] = async {
    var _fs = fs
    val r = ListBuffer[T]()

    while (_fs != Nil) {
      r.append(await { _fs.head })
      _fs = _fs.tail
    }
    r.toList
  }

  def recursiveSequence[T](fts: List[Future[T]]): Future[List[T]] = {
//    fts match {
//      case Nil => Future(Nil)
//      case (ft::fts) => ft.flatMap(t => recursiveSequence(fts)
//                            .flatMap(ts => Future(t::ts)))
//    }

      fts match {
        case Nil => Future(Nil)
        case (ft::fts) =>
          for {
            t <- ft
            ts <- recursiveSequence(fts)
          } yield t::ts
      }
  }

  test("sequence test 1") {
    val w = new Waiter

    val result: Future[List[String]] = recursiveSequence(randomFutures)
    result.onComplete {
      case Success(xs) =>
        xs should be ((1 to 10).toList.map(_.toString))
        w.dismiss()
      case _ => fail(); w.dismiss()
    }

    w.await(limit)
  }

  test("sequence test 0") {
    val w = new Waiter

    val result: Future[List[String]] = awaitSequence(randomFutures)
    result.onComplete {
      case Success(xs) =>
        xs should be ((1 to 10).toList.map(_.toString))
        w.dismiss()
      case _ => fail(); w.dismiss()
    }

    w.await(limit)
  }

}
