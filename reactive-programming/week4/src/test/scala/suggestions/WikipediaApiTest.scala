package suggestions



import language.postfixOps
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Try, Success, Failure}
import rx.lang.scala._
import org.scalatest._
import gui._

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner


@RunWith(classOf[JUnitRunner])
class WikipediaApiTest extends FunSuite with Matchers {

  object mockApi extends WikipediaApi {
    def wikipediaSuggestion(term: String) = Future {
      if (term.head.isLetter) {
        for (suffix <- List(" (Computer Scientist)", " (Footballer)")) yield term + suffix
      } else {
        List(term)
      }
    }
    def wikipediaPage(term: String) = Future {
      "Title: " + term
    }
  }

  import mockApi._

  test("WikipediaApi should make the stream valid using sanitized") {
    val notvalid = Observable.just("erik", "erik meijer", "martin")
    val valid = notvalid.sanitized

    var count = 0
    var completed = false

    val sub = valid.subscribe(
      term => {
        assert(term.forall(_ != ' '))
        count += 1
      },
      t => assert(false, s"stream error $t"),
      () => completed = true
    )
    assert(completed && count == 3, "completed: " + completed + ", event count: " + count)
  }

  test("WikipediaAPI should recover from errors") {
    case class ex1(msg: String) extends RuntimeException

    val obs: Observable[Int] = Observable.create { o =>
      o.onNext(1)
      o.onNext(2)
      o.onError(ex1("exception1"))
      Subscription { o.onCompleted() }
    }

    val result = obs.recovered.toBlocking.toList
    result should be (List(Success(1), Success(2), Failure(ex1("exception1"))))
  }

  test("WikipediaApi should correctly use concatRecovered") {
    val requests = Observable.just(1, 2, 3)
    val remoteComputation = (n: Int) => Observable.just(0 to n : _*)
    val responses = requests concatRecovered remoteComputation
    val sum = responses.foldLeft(0) { (acc, tn) =>
      tn match {
        case Success(n) => acc + n
        case Failure(t) => throw t
      }
    }
    var total = -1
    val sub = sum.subscribe {
      s => total = s
    }
    assert(total == (1 + 1 + 2 + 1 + 2 + 3), s"Sum: $total")
  }

  test("concatRecovered spec1") {
    case class ex1(msg: String) extends RuntimeException

    val o1 = Observable.from(List(1, 2, 3, 4))
    val o2 = o1.concatRecovered(x => if (x == 3) Observable.error(ex1("ex")) else Observable.just(x))


    val result = o2.toBlocking.toList

    result should be (List(Success(1), Success(2), Failure(ex1("ex")), Success(4)))
  }

  test("concatRecovered spec2") {
    val o1 = Observable.from(List(1, 2))
    val o2 = o1.concatRecovered(x => Observable.from(List(x, x)))

    val result = o2.toBlocking.toList
    result should be (List(Success(1), Success(1), Success(2), Success(2)))
  }

  // ref: https://class.coursera.org/reactive-002/forum/thread?thread_id=700
  test("There should be only 1 element before timeout") {
    val o = Observable.from(List(1, 2, 3)).zip(Observable.interval(700 millis))

    o.subscribe(x => println(x))
    Thread.sleep(800)

    val r = o.timedOut(1L)
    val elements = r.toBlocking.toList
    elements should be (List((1, 0)))
  }

  // ref: http://reactivex.io/documentation/operators.html#connectable
  // ref: http://leecampbell.blogspot.kr/2010/08/rx-part-7-hot-and-cold-observables.html

  ignore ("ConnectableObservable Exploit2") {
    val hot = Observable.interval(200 millis).publish
    hot.connect

    Thread.sleep(500)

    val r = hot.replay
    r.connect

    r.subscribe(x => println(s"Observer1 : $x"))
    Thread.sleep(200)
    r.subscribe(x => println(s"Observer2 : $x"))

    hot.take(600 millis).toBlocking.toList
  }

  ignore ("ConnectableObservable Exploit1") {
    val o = Observable.interval(200 millis).take(1 seconds).publish /* create connectable observable */
    o.connect /* emit items to its subscribers */
    o.subscribe(x => println(s"Observer 1: $x"))
    Thread.sleep(500)
    o.subscribe(x => println(s"Observer 2: $x"))

    o.take(1 seconds).toBlocking.toList
  }

}
