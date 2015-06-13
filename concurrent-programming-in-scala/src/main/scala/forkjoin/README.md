# Fork Join Framework

## Executor, ExecutorService, ForkJoinPool

`Executor` objects serve to decouple the logic in the concurrent computations from 
how these computations are executed. The programmer can focus on specifying parts of the code that 
potentially execute concurrently, separately from where and when to execute those parts of the code.

The more elaborate subtype of the Executor interface, also implemented by the `ForkJoinPool` class is called 
`ExecutorService`. This extended `Executor` interface defines several convenient methods.

[Ref - Interface ExecutorService](http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ExecutorService.html)

```java
public class ForkJoinPool extends AbstractExecutorService {
    ...
    ...
}
```

To ensure that all the tasks submitted to the `ForkJoinPool` object are complete, we need to additionally 
call the `awaitTermination` method, specifying the maximum amount of time to wait for their completion 
instead of calling the `sleep` statement.

```scala
import java.util.concurrent.TimeUnit

executor.shutdown()
executor.awaitTermination(60, TimeUnit.SECONDS)
```

## ExecutionContext 

The `scala.concurrent` package defines the `ExecutionContext` trait that offers a similar functionality to that of `Executor` 
objects but is more specific to Scala. 

[Ref - Scala ExecutionContext Source](https://github.com/scala/scala/blob/2.11.x/src/library/scala/concurrent/ExecutionContext.scala)

> An `ExecutionContext` can execute program logic asynchronously, typically but not necessarily on a thread pool. 
> APIs such as `Function.onComplete` require you to provide a callback and an implicit `ExecutionContext`.  
> The implicit `ExecutionContext` will be used to execute the callback.

[Ref - Scala Future.onComplete Source](https://github.com/scala/scala/blob/2.11.x/src/library/scala/concurrent/Future.scala#L149)

```scala
def onComplete[U](@deprecatedName('func) f: Try[T] => U)(implicit executor: ExecutionContext): Unit
```

It is possible to simply import `scala.concurrent.ExecutionContext.Implicits.global` to obtain an implicit `ExecutionContext`. 
This global context is a reasonable default thread pool.

A custom `ExecutionContext` may be appropriate to execute code which blocks on IO or performs long-running computations. 
`ExecutionContext.fromExecutorService` and `ExecutionContext.fromExecutor` are good ways to create a custom `ExecutionContext`.

```scala
// https://github.com/scala/scala/blob/2.11.x/src/library/scala/concurrent/ExecutionContext.scala

trait ExecutionContext {
  def execute(runnable: Runnable): Unit
  def reportFailure(@deprecatedName('t) cause: Throwable): Unit
  def prepare(): ExecutionContext = this
}

object ExecutionContext {
  def global: ExecutionContextExecutor = Implicits.global

  object Implicits {
    implicit lazy val global: ExecutionContextExecutor = impl.ExecutionContextImpl.fromExecutor(null: Executor)
  }

  def fromExecutorService(e: ExecutorService, reporter: Throwable => Unit): ExecutionContextExecutorService =
    impl.ExecutionContextImpl.fromExecutorService(e, reporter)

  def fromExecutorService(e: ExecutorService): ExecutionContextExecutorService = fromExecutorService(e, defaultReporter)

  def fromExecutor(e: Executor, reporter: Throwable => Unit): ExecutionContextExecutor =
    impl.ExecutionContextImpl.fromExecutor(e, reporter)

  def fromExecutor(e: Executor): ExecutionContextExecutor = fromExecutor(e, defaultReporter)

  def defaultReporter: Throwable => Unit = _.printStackTrace()
}
```

## ExecutionContext example

```scala
object ExecutorCreate extends App with ThreadUtils {
  val executor = new ForkJoinPool

  val a = new ExecutionContext {override def reportFailure(cause: Throwable): Unit = ???

    override def execute(runnable: Runnable): Unit = ???
  }

  executor.execute(new Runnable {
    override def run(): Unit = log("task is run async")
  })

  Thread.sleep(500);
}

object ExecutionContextCreate extends App with ThreadUtils {
  val pool = new ForkJoinPool(2)
  val ectx = ExecutionContext.fromExecutorService(pool)

  ectx.execute(new Runnable {
    override def run(): Unit = log("task is run async")
  })

  Thread.sleep(500);
}

trait ExecutorUtils {
  def execute(body: => Unit) = ExecutionContext.global.execute(
    new Runnable {
      override def run(): Unit = body
    }
  )
}
```

`ExecutionContext` can improve throughput by reusing the same set of threads for different tasks but are unable to execute tasks 
if those threads become unavailable.

```scala
object ExecutionContextSleep extends App with ExecutorUtils with ThreadUtils{
  for(i <- 0 until 32) execute {
    Thread.sleep(2000)
    log(s"Task $i compelted.")
  }

  Thread.sleep(10000)
}

// output
> runMain forkjoin.ExecutionContextSleep
[info] Running forkjoin.ExecutionContextSleep 
ForkJoinPool-2-worker-3: Task 7 completed.
ForkJoinPool-2-worker-15: Task 1 completed.
ForkJoinPool-2-worker-5: Task 6 completed.
ForkJoinPool-2-worker-13: Task 0 completed.
ForkJoinPool-2-worker-11: Task 3 completed.
ForkJoinPool-2-worker-9: Task 4 completed.
ForkJoinPool-2-worker-7: Task 2 completed.
ForkJoinPool-2-worker-1: Task 5 completed.
ForkJoinPool-2-worker-5: Task 10 completed.
ForkJoinPool-2-worker-1: Task 15 completed.
ForkJoinPool-2-worker-7: Task 14 completed.
ForkJoinPool-2-worker-11: Task 12 completed.
ForkJoinPool-2-worker-9: Task 13 completed.
ForkJoinPool-2-worker-13: Task 11 completed.
ForkJoinPool-2-worker-3: Task 8 completed.
ForkJoinPool-2-worker-15: Task 9 completed.
ForkJoinPool-2-worker-11: Task 19 completed.
ForkJoinPool-2-worker-3: Task 22 completed.
ForkJoinPool-2-worker-9: Task 20 completed.
ForkJoinPool-2-worker-15: Task 23 completed.
ForkJoinPool-2-worker-5: Task 16 completed.
ForkJoinPool-2-worker-1: Task 17 completed.
ForkJoinPool-2-worker-7: Task 18 completed.
ForkJoinPool-2-worker-13: Task 21 completed.
ForkJoinPool-2-worker-5: Task 28 completed.
ForkJoinPool-2-worker-13: Task 31 completed.
ForkJoinPool-2-worker-7: Task 30 completed.
ForkJoinPool-2-worker-11: Task 24 completed.
ForkJoinPool-2-worker-3: Task 25 completed.
ForkJoinPool-2-worker-9: Task 26 completed.
ForkJoinPool-2-worker-15: Task 27 completed.
ForkJoinPool-2-worker-1: Task 29 completed.
[success] Total time: 10 s, completed Jun 13, 2015 1:30:50 PM
```

On quad-core CPU with hyper threading, the global `ExecutionContext` object has eight threads in the thread pool. 
so it executes work tasks in batches of eight.

This is because the global `ExecutionContext` object internally maintain a pool of eight worker threads, 
and calling `sleep` puts all of them int a timed waiting state.

Things can be much works. If we start eight threads using guarded block (`wait` method), and another task 
call `notify` to wake them up. As `ExecutionContext` object can execute only eight thread concurrently, the worker 
threads would be blocked forever. We say that executing blocking operations on `ExecutionContext` objects can cause **starvation**.

> Avoid executing operations that might block indefinitely on `ExecutionContext` and `Executor` objects.

## Atomic primitives

```scala
def genUniqueUid() = this.synchronized {
  val freshUid = uidCount + 1
  uidCount = freshUid
  freshUid
}
```

Volatile fields were a more light weight way of ensuring happens-before relationships. but a less powerful 
synchronization construct. Recall how volatile fields alone could not implement the `genUniqueId` method correctly.

**Atomic variables** are volatile variables' close cousins but are more expressive than volatile variables. 
They are used to build complex concurrent operations without relying on the synchronized statement.

## Atomic Variables

An *atomic variable** is a memory location that supports complex **linearizable** operations. 
A **linearizable operation** is any operation that appears to occur instantaneously to the rest of the system. 
For example, a volatile write is a linearizable operation. 

A **complex linearizable operation** is a linearizable operation equivalent to at least two reads and/or writes. 

`genUniqueId` can be reimplemented using atomic long variables 

```scala
object AtomicUid extends App with ThreadUtils with ExecutorUtils {
  private val uid = new AtomicLong(0L)

  def genUniqueId(): Long = uid.incrementAndGet()

  execute { log(s"Uid asynchronously: ${genUniqueId()}")}

  log(s"Got a unique: id ${genUniqueId()}")
}
```

Atomic variables define these methods

- `getAndSet`
- `decrementAndGet`
- `addAndGet`

In turns out that all these atomic operations can be implemented in terms of **compare-and-set(swap)** operation (CAS), 
takes the expected previous value and the new value for the atomic variable and atomically replaces the current value with the new value 
only if the current value is equal to the expected value.

The CAS operation is a fundamental building block for lock-free programming. The CAS operation conceptually equivalent to the 
following synchronized block but is more efficient and does not get blocked on most JVMs, as it is implemented in terms of 
processor instruction.


```scala
def compareAndSet(expectedCurrentValue: T, newValue: T): Boolean = this.synchronized {
  if (this.get != expectedCurrentValue) false else {
    this.set(newValue)
    true
  }
}
```

Now we can re-implement `genUniqueId()` using `compareAndSet`.

```scala
object UidImplementUsingCAS extends App with ThreadUtils with ExecutorUtils {
  private var uid = 0L

  def compareAndSet(currentValue: Long, newValue: Long): Boolean = this.synchronized {
    if (currentValue != this.uid) false else {
      this.uid = newValue
      true
    }
  }

  @tailrec def genUniqueId(): Long = {
    val oldUid = uid
    val newUid = uid + 1
    if (compareAndSet(oldUid, newUid)) newUid else genUniqueId()
  }

  for(i <- 0 until 1000) yield thread {
    log(s"uid: ${genUniqueId()}")
    Thread.sleep(20);
  }

  Thread.sleep(1000);
}

// using an atomic reference
object UidImplUsingAtomicRefAndCAS extends App with ThreadUtils with ExecutorUtils {
  private val uid = new AtomicLong(0L)

  @tailrec def genUniqueId(): Long = {
    val oldUid = uid.get
    val newUid = oldUid + 1
    if (uid.compareAndSet(oldUid, newUid)) newUid else genUniqueId()
  }

  for(i <- 0 until 1000) yield thread {
    log(s"uid: ${genUniqueId()}")
    Thread.sleep(20);
  }

  Thread.sleep(1000);
}
```

Retrying is a common pattern when programming with CAS operations. This retry can happen infinitely many times. 
The good news id that a CAS in Thread `T` can fail only when another thread `S` completes the operation successfully. 

If our part of the system does not progress, at least some other part of the system does. In fact, the `genUniqueId` method is fair 
to all the threads in practice, and most JDKs implement `incrementAndGet` in a very similar manner.















