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

### Interrupting threads and graceful shutdown

> Calling `interrupt` method on a thread that is in the waiting or timed waiting state causes it to throw an `InterruptedException` 
This exception can be handled and caught. However, If we call this method while the thread is 
running, the exception is not thrown and he thread's interrupt flag is set. A thread that does not block must 
periodically query the interrupt flag with the `isInterrupted` method. 

Alternatively, We can implement **graceful shutdown**

```scala
object GracefulShutdown extends App with ThreadUtils {
  import scala.collection._

  private type Task = () => Unit
  private val tasks = mutable.Queue[Task]()

  object Worker extends Thread {
    var terminated = false;

    def poll(): Option[Task] = tasks.synchronized {
      while(!terminated && tasks.isEmpty) tasks.wait()
      if (!terminated) Some(tasks.dequeue()) else None
    }

    @tailrec override def run() = poll() match {
      case Some(task) => task(); run();
      case None =>
    }

    def shutdown() = tasks.synchronized {
      terminated = true
      tasks.notify()
    }
  }

  Worker.start()

  def async(block: => Unit) = tasks.synchronized {
    tasks.enqueue(() => block)
    tasks.notify()
  }

  async { log("Hello World") }
  async { log("Hello scala!") }

  Thread.sleep(1000)

  Worker.shutdown()
}
```

> The situation where calling `interrupt` is preferred to a graceful shutdown is when 
we cannot wake the thread using `notify`. One example is when the thread does blocking I/O 
on an `InterruptibleChannel` object, in which case the object the thread is calling the wait method on is hidden

### Volatile

**Volatile** variables can be atomically read and modified, and mostly used as status flag. 
Reads and writes to variables marked as volatile are never reordered.

```scala
object VolatileExample extends App with ThreadUtils {
  class Page(val text: String, var position: Int)

  val pages = for(i <- 1 to 10) yield
    new Page("Na" * (10000000 - 20 * i) + " Batman!", -1)

  @volatile var found = false

  for(p <- pages) yield thread {
    var i = 0

    while(i < p.text.length && !found) {
      if (p.text(i) == '!') {
        p.position = i
        found = true
      } else i += 1
    }
  }

  while (!found) {}
  log(s"position: ${pages map (_.position)}}")
}
```

> Unlike Java, Scala allows you to declare **local fields** volatile. A heap object 
with a volatile field is created for each local volatile variable used in some 
closure or a nested class. We say the variable is **lifted** into an object

### Java Memory Model

A language memory model is a specification that describes the circumstance under 
which a write to a variable become visible to other threads. 

If a write to a variable `v` changes the corresponding memory location immediately after 
the processor executes it, and that other processors see the new value of `v` instantaneously, 
Then it is called **sequential consistency**.

In fact, writes rarely end up in the main memory immediately. instead, the processors 
have hierarchies of caches that ensure a better performance. 

Also, compilers are allowed to use registers to postpone or avoid memory writes, and 
reorder statement to achieve optimal performance, as long as it does not change the serial semantics.

> A memory model is a trade-off between the predictable behavior of a concurrent program and a 
compiler's ability to perform optimizations. Not every language or platform has a memory model. 
A typical purely functional programming language, which doesn't support mutations, does not need a memory model at all.

Scala inherits its memory model from the JVM, which precisely specifies a set of **happens-before** relationships 
between different actions in a program.

The same set of happens-before relationships is valid for the same program irrespective of the machine it runs on. 
It is the JVM's task to ensure this.

The happens-before relationship ensues that nonvolatile reads and wirtes also cannot be reordered arbitrarily.

- A non-volatile read cannot be reordered to appear before a volatile read (or monitor lock) that precedes it in the program order
- A non-volatile write cannot be reordered to appear before a volatile write (or monitor lock) that precedes it in the program order

**It is the task of programmer to ensure that every write of a variable is in a happens-before relationship with every read of that variable that should read the written value**.

### Immutable Objects and Final Fields

```scala
class Foo(final val a: Int, val b: Int)

// will be translated by Scala compiler

class Foo {
  final private int a$;
  final private int b$;
  final public int a() { return a$; }
  
  public int b() { return b$; } // can be overrided
  public Foo(int a, int b) {
    a$ = a;
    b$ = b;
  }
  
}
```

If the `t` thread see the `inc` is not null, invoking `inc` still works correctly, because the `$number` 
field is appropriately initialized snice it is stored as a field in the immutable lambda object. 

The Scala compiler ensures that lambda values contain only final, properly initialized fields.

- Anonymous classes
- Auto-boxed primitives
- Value classes share the same philosophy.

```scala
var inc: () => Unit = null
val t = thread { if (inc != null) inc() }
private var number = 1
inc = () => { number += 1 }

// will be translated

number = new IntRef(1)

inc = new Function0 {
  val $number = number // vals are final
  def apply() = $number.elem += 1
}
```

The local `number` variable is capured by the lambda, so it needs to be lifted.

> In current version of Scala, however, certain collections that are deemed immutable, such as 
`List` and `Vector` cannot be shared without synchronization. Although their external API does not allow you to 
modify them, they contain non-final fields.






