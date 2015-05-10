package lecture

import org.junit.runner.RunWith
import org.scalatest.{Matchers, FunSuite}
import org.scalatest.junit.JUnitRunner
import rx.lang.scala.subjects.{BehaviorSubject, ReplaySubject, AsyncSubject, PublishSubject}

@RunWith(classOf[JUnitRunner])
class SubjectSpec extends FunSuite with Matchers {

  def record(s: String): Int => Unit = (x) => println(s"$s: $x")

  test("behavior subject") {
    val channel = BehaviorSubject[Int]()

    val a = channel.subscribe(record("a"))

    channel.onNext(31)
    channel.onNext(32)

    val b = channel.subscribe(record("b"))

    // a: 31, 32 b: 32
    channel.onCompleted()
  }

  ignore ("replay subject") {
    val channel = ReplaySubject[Int]()

    val a = channel.subscribe(record("a"))

    channel.onNext(31)
    channel.onNext(32)

    channel.onCompleted()

    // a: 31, 32 b: 31, 32
    val b = channel.subscribe(record("b"))
  }

  ignore ("async subject") {
    val channel = AsyncSubject[Int]()

    val a = channel.subscribe(record("a"))
    channel.onNext(37)
    channel.onNext(38)

    // values passed into onNext will not emit until calling onCompleted
    // a:38, b:38.

    // AsyncSubject caches final value
    channel.onCompleted()
    val b = channel.subscribe(record("b"))
  }

  ignore ("publish subject") {
    val channel = PublishSubject[Int]()

    val a = channel.subscribe(record("a"))
    val b = channel.subscribe(record("b"))

    channel.onNext(42)

    a.unsubscribe()

    channel.onNext(4711)

    // has no effect on subscriptions in the channel even we add new subs
    channel.onCompleted()
    channel.onNext(13)
    val c = channel.subscribe(record("c"))

    a.isUnsubscribed should be (true)
    c.isUnsubscribed should be (false) // not unsubscribed yet.
  }

}
