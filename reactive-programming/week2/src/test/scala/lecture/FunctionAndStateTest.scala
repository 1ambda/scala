package lecture

import org.junit.runner.RunWith
import org.scalatest.{ShouldMatchers, FunSuite}
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class FunctionsAndStateTest extends FunSuite with ShouldMatchers {

  test("throw insufficient funds exception") {
    val acc = new BankAccount
    acc deposit 50
    acc withdraw 20
    acc withdraw 20

    intercept[Error] {
      acc withdraw 20 // throw exception
    }
  }

}

class BankAccount {

  private var balance = 0

  def deposit(amount: Int) = {
    if (amount > 0) balance += amount
  }

  def withdraw(amount: Int) = {
    if (0 < amount && amount <= balance) {
      balance -= amount
      balance
    } else throw new Error("insufficient funds")
  }
}

class BankAccountProxy(ba: BankAccount) {
  def deposit(amount: Int) = ba.deposit(amount)
  def withdraw(amount: Int) = ba.withdraw(amount)
}
