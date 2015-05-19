package lecture.bank

import akka.actor.{Actor, Props}
import akka.event.LoggingReceive

class TransferApp extends Actor {
  val accountA = context.actorOf(Props[BankAccount], "accountA")
  val accountB = context.actorOf(Props[BankAccount], "accountB")

  accountA ! BankAccount.Deposit(100)

  def receive = LoggingReceive {
    case BankAccount.Done => transfer(150)
  }

  def transfer(amount: BigInt): Unit = {
    val transaction = context.actorOf(Props[WireTransfer], "transfer")
    transaction ! WireTransfer.Transfer(accountA, accountB, amount)

    context.become(LoggingReceive {
      case WireTransfer.Done =>
        println("success")
        context.stop(self)

      case WireTransfer.Failed =>
        println("failed")
        context.stop(self)
    })
  }
}
