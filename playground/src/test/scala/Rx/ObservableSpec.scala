package Rx

import org.junit.runner.RunWith
import org.scalatest.{Matchers, FunSuite}
import org.scalatest.junit.JUnitRunner

import rx.lang.scala._
import rx.lang.scala.schedulers.{ComputationScheduler, IOScheduler}

import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class ObservableSpec extends FunSuite with Matchers {

  def range(b: Int) = (b to b + 4).toList.map(_.toString)
  def generator(base: Int) = Observable.from(range(base)).debug("Generated")

  def action(a: String): (String => String) = (value) => value + a
  def plus = action("+")
  def minus = action("-")

  val inc = generator(1).map(plus).debug("plus")
  val dec = inc.map(minus).debug("minus")

  ignore ("subscribeOn return new observable") {
    generator(1).subscribeOn(IOScheduler()).subscribe(x => x)
  }

  test("delay function use `subscribeOn`") {
    val inc = generator(1).map(plus).debug("plus")
    val delayed = inc.delay(Duration(1, MILLISECONDS)).debug("delayed")
    val dec = delayed.map(minus).debug("minus")

    dec.subscribe(x => x)
  }

  ignore ("observeOn + observeOn") {
    val inc = generator(1).subscribeOn(IOScheduler()).map(plus).debug("plus")
    val dec = inc.observeOn(IOScheduler()).map(minus).debug("minus")

    dec.subscribe(x => x)
  }

  ignore ("observeOn + subscribeOn") {
    val inc = generator(1).subscribeOn(ComputationScheduler()).map(plus).debug("plus")
    val dec = inc.observeOn(IOScheduler()).map(minus).debug("minus")

    dec.subscribe(x => x)
  }

  ignore ("observeOn") {
    generator(1).subscribeOn(IOScheduler()).observeOn(ComputationScheduler())
      .map(plus).debug("plus").subscribe(x => x)
  }


  ignore ("run on IoScheduler") {
    generator(1).subscribeOn(IOScheduler()).subscribe(x => x)
    generator(11).subscribeOn(IOScheduler()).subscribe(x => x)
  }

  ignore ("plus then minus") {
    dec.subscribe(x => x)
  }

  ignore ("plus") {
    inc.subscribe(x => x)
  }

  ignore ("debug should print msg") {
    generator(1).map(x => x + 1).debug("mapped").subscribe(x => x)
  }
}
