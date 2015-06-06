package thread

import scala.collection._

class MonitorsAndSynchronization {}

object SynchronizedNesting extends App with ThreadUtils {
  private val transfers = mutable.ArrayBuffer[String]()

  def logTransfer(name: String, amount: Int) = transfers.synchronized {
    transfers += s"transfer to account '$name' = $amount"
  }

  class Account(val name: String, var balance: Int)

  def add(account: Account, amount: Int) = account.synchronized {
    account.balance += amount
    if (amount > 10) logTransfer(account.name, amount)
  }

  val acc1 = new Account("Jane", 100)
  val acc2 = new Account("John", 100)

  val t1 = thread { add(acc1, 5) }
  val t2 = thread { add(acc2, 50) }
  val t3 = thread { add(acc1, 70) }

  t1.join(); t2.join(); t3.join()
  log(s"--- transfers ---\n $transfers")
}
