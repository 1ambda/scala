package lecture.FRP

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, ShouldMatchers}

import scala.util.DynamicVariable

@RunWith(classOf[JUnitRunner])
class FRP extends FunSuite with ShouldMatchers {
  def consolidate(acs: List[BankAccount]): Signal[Int] =
    Signal(acs.map(_.balance()).sum)

  test("FRP") {
    val a = new BankAccount()
    val b = new BankAccount()
    val c = consolidate(List(a, b))

    assert(c() == 0)

    a deposit 20
    assert(c() == 20)

    b deposit 30
    assert(c() == 50)

    val xchange = Signal(246.00)
    val inDollar = Signal(c() * xchange())
    assert(inDollar() == 12300.00)

    b withdraw 10
    assert(inDollar() == 9840.0)
  }
}

class StackableVariable[T](init: T) {
  private var values: List[T] = List(init)

  def value: T = values.head
  def withValue[R](newValue: T)(op: => R): R = {
    // you can think of newValue as the caller
    values = newValue :: values
    try op finally values = values.tail
  }
}

// sentinel signal object
object NoSignal extends Signal[Nothing](???) {
  override def computeValue() = ()
}

object Signal {
  // private val caller = new StackableVariable[Signal[_]](NoSignal)
  private val caller = new DynamicVariable[Signal[_]](NoSignal)
  def apply[T](expr: => T) = new Signal(expr)
}

class Signal[T](expr: => T) {
  import Signal._

  private var myExpr: () => T = _
  private var myValue: T = _
  private var observers: Set[Signal[_]] = Set()
  update(expr)

  protected def update(expr: => T): Unit = {
    myExpr = () => expr
    computeValue()
  }

  // update all observers with newly calculated `newValue`
  protected def computeValue(): Unit = {
    // 1. add `this` to `caller.values` so that signals in `myExpr` can use `this` as caller
    // 2. execute `myExpr`. signals in `myExpr` will add this into `observers`
    val newValue = caller.withValue(this)(myExpr())

    if (newValue != myValue) {
      myValue = newValue
      val obs = observers
      // computeValue will call observer's `myExpr`
      // then, this.apply() also will be called.
      // so we need to clear this.observers before to avoid duplication
      observers = Set()
      obs foreach { _.computeValue() }
    }
  }

  // add caller, and return `myValue`
  def apply(): T = {
    observers += caller.value
    assert(!caller.value.observers.contains(this), "cylick signal definition")
    myValue
  }
}

object Var {
  def apply[T](expr: => T) = new Var(expr)
}

class Var[T](expr: => T) extends Signal[T](expr) {
  override def update(expr: => T): Unit = super.update(expr)
}

class BankAccount {
  var balance = Var(0)

  def deposit(amount: Int): Unit =  if (amount > 0){
    // avoid cyclic definition
    val b = balance()
    balance() = b + amount
  }

  def withdraw(amount: Int): Unit = {
    if (0 < amount && amount <= balance()) {
      val b = balance()
      balance() = b - amount
    }
    else throw new Error("insufficient funds")
  }
}


