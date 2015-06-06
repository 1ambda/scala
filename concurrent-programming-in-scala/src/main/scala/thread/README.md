## Race Condition

A **race condition** is a phenomenon in which the output of a concurrent program depends on the execution schedule 
of the statements in the program

## Synchronization Block

We can also call `synchronized` and omit `this` part, in which case the compiler wil infer 
what the surrounding object is, but we strongly discourage you from doing so.  
Synchronization on incorrect objects results in concurrency errors that are not easily identified.

The JVM ensures that the thread executing a `synchronized` statement invoked on some `x` object is 
the only thread executing any `synchronized` statement on that particular object `x` 

[SO - Java Synchronized Method Lock on Object or Method?](http://stackoverflow.com/questions/3047564/java-synchronized-method-lock-on-object-or-method)

> per Object, but not synchronized method are always available.

> Locking is only at synchronized method level and object's instance variables can be accessed by other thread


### Monitor, Intrinsic Lock

Every object created inside the JVM has a special entity called an **intrinsic lock** or 
a **monitor**, which is used to ensure that only one thread is executing some `synchronized` 
block on that object.

## Reordering

Why we can't reason about the execution of the program the way we did? The answer is that by the JMM specification, 
the JVM is allowed to reorder certain program statements executed by one thread as long as 
it does not change the serial semantics of the program for that particular thread.

This is because some processors do not always execute instructions in the program order. 
Additionally, the threads do not need to write all their updates to the main memory immediately, 
but can temporarily kepp them cached in registers inside the processors. This maximize the 
efficiency of the program and allows better compiler optimizations.

## Deadlock

A **deadlock** is a general situation in which two or more executions wait for each other 
to complete an action before proceeding with their own action. 

> A deadlock occurs when a set of two or more threads acquire resources and then cyclically try to 
acquire other thread's resources without releasing their own.

To resolve **deadlock**, establish a total order between resources when acquiring them. 
This ensures that no set of threads cyclically wait on the resources they previously acquired.

```scala
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
```

## Guarded Blocks

### Daemon Thread

[Ref - What is Daemon Thread in Java](http://javarevisited.blogspot.kr/2012/03/what-is-daemon-thread-in-java-and.html)

> Daemon Thread are treated differently than User Thread when JVM terminates, finally blocks are not called, Stacks are not unwounded and JVM just exits.

### Spurious Wakeup

Occasionally, the JVM is allowed to wake up a thread that called `wait` even though there is no  
corresponding `notify` call. To guard against this, we must always use `wait` in conjunction with  
a `while` loop that checks the condition

```scala
```
 






