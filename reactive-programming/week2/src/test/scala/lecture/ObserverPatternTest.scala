package lecture.ObserverPatternTest

import org.junit.runner.RunWith
import org.scalatest.{ShouldMatchers, FunSuite}
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ObserverPatternTest extends FunSuite with ShouldMatchers {
  test("imperative event hanlding") {
    val a = new BankAccount
    val b = new BankAccount
    val c = new Consolidator(List(a, b))

    c.totalBalance
    a deposit 20
    assert(c.totalBalance == 20)
  }
}

trait Subscriber {
  def handler(pub: Publisher)
}

trait Publisher {

  private var subscribers: Set[Subscriber] = Set()

  def subscribe(subscriber: Subscriber): Unit = {
    subscribers += subscriber
  }

  def unsubscribe(subscriber: Subscriber): Unit = {
    subscribers -= subscriber
  }

  def publish(): Unit = {
    subscribers foreach { _.handler(this) }
  }
}

class BankAccount extends Publisher {
  private var balance = 0
  def currentBalance = balance

  def deposit(amount: Int): Unit = {
    if (amount > 0) balance += amount
    publish()
  }

  def withdraw(amount: Int): Unit = {
    if (0 < amount && amount <= balance) {
      balance -= balance
      publish()
    } else throw new Error("insufficient funds")
  }
}

class Consolidator(observed: List[BankAccount]) extends Subscriber {
  private var total: Int = _ // initialize as uninitialized. (!!)
  def totalBalance = total

  private def compute(): Unit =  total = observed.map(_.currentBalance).sum
  override def handler(pub: Publisher): Unit =  compute()

  observed foreach { _.subscribe(this) }
  compute()
}
