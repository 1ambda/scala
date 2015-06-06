package thread

class DeadLock {}

object SynchronizedDeadlock extends App with ThreadUtils {
  import SynchronizedNesting.Account

  def send(a: Account, b: Account, amount: Int) = a.synchronized {
    b.synchronized {
      a.balance -= amount
      b.balance += amount
    }
  }

  val a = new Account("Jack", 1000)
  val b = new Account("Jill", 2000)

  val t1 = thread { for (i <- 0 until 100) send(a, b, 1) }
  val t2 = thread { for (i <- 0 until 100) send(b, a, 1) }

  t1.join(); t2.join();
  log(s"a = ${a.balance}, b = ${b.balance}")
}

object SynchronizedWithoutDeadlock extends App with ThreadUtils {
  import ThreadProtectedUid.genUniqueUid

  class Account(val name: String, var balance: Int) {
    val uid = genUniqueUid()
  }

  def send(a: Account, b: Account, amount: Int): Unit = {
    def adjust(): Unit = {
      a.balance -= amount
      b.balance += amount
    }

    if (a.uid < b.uid) a.synchronized { adjust() }
    else               b.synchronized { adjust() }
  }

  val a = new Account("Jack", 1000)
  val b = new Account("Jill", 2000)

  val t1 = thread { for (i <- 0 until 1000) send(a, b, 1) }
  val t2 = thread { for (i <- 0 until 1000) send(b, a, 1) }

  t1.join(); t2.join();
  log(s"a = ${a.balance}, b = ${b.balance}")
}

