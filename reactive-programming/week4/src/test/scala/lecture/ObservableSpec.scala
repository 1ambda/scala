package lecture

import org.junit.runner.RunWith
import org.scalatest.concurrent.AsyncAssertions
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import scala.concurrent.duration._
import rx.lang.scala._

@RunWith(classOf[JUnitRunner])
class ObservableSpec extends FunSuite with Matchers with AsyncAssertions {

  val limit = timeout(2 seconds)

  // ref: https://github.com/ReactiveX/RxScala/blob/0.x/examples/src/test/scala/rx/lang/scala/examples/RxScalaDemo.scala#L837
  def waitFor[T](obs: Observable[T]): Unit = {
    obs.toBlocking.toIterable.last
  }

  def ticks = Observable.interval(100 millis)

  test("concat example") {
    val xs = Observable.from(List(3, 2, 1))
    val yss = xs.map(x => Observable.interval(x millis).map(_ => x).take(2))
    val zs = yss.concat
    val result = zs.toBlocking.toList
    result should be (List(3, 3, 2, 2, 1, 1))
  }

  test("flatten example") {
    val xs = Observable.from(List(3, 2, 1))
    val yss = xs.map(x => Observable.interval(x millis).map(_ => x).take(2))

    val zs = yss.flatten
    zs.subscribe(Nil) // do nothing

    zs.toBlocking.toList should (
      equal (List(1, 2, 1, 3, 2, 3)) or
      equal (List(1, 1, 2, 3, 2, 3))
    )
  }

  test("slidingBuffer example") {
    val evens = ticks.filter(_ % 2 == 0)
    val bufs = evens.slidingBuffer(count = 2, skip = 3)
    val obs = bufs.take(4)

    obs.subscribe(println(_))
    waitFor(obs)
  }

  test("basic async observable") {
    val w = new Waiter

    val subs = ticks.take(5).subscribe(
      n => println(n),
      e => e.printStackTrace(),
      () => w.dismiss()
    )

    w.await(limit)
  }

  test("basic blocking observable") {
    val obs = ticks.map(x => x + 10).take(5)
    obs.subscribe(
      n => println(n),
      e => e.printStackTrace(),
      () => println("done")
    )

    waitFor(obs)
  }
}
