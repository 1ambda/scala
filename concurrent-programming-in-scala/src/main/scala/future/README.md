# Future

> The `Future[T]` type encodes latency in the program. Use it to encode values that will 
become available later during execution.

```scala
object FuturesCallbacks extends App with ThreadUtils with ExecutorUtils {

  def getUrlSpec(): Future[List[String]] = Future {
    val url = "http://www.w3.org/Addressing/URL/url-spec.txt"
    val f = Source.fromURL(url)

    try f.getLines().toList finally f.close();
  }

  def find(lines: List[String], keyword: String): String =
    lines.zipWithIndex collect {
      case (line, n) if line.contains(keyword) => (n, line)
    } mkString("\n")

  val urlSpec:Future[List[String]] = getUrlSpec()
  urlSpec foreach {
    case lines => log(find(lines, "telnet"))
  }

  log("callback registered, continuing with other work")
  Thread.sleep(2000)
}
```

Note that the callback is **not necessarily invoked immediately** after the future is completed. 
Most execution contexts schedule a task to asynchronously process the callbacks. The same is true if 
the future is already completed when we try to install a callback.
 
The specified execution context decides when and on which thread the callback gets executed.
 
There is a happens-before relationship between completing the future and starting the callback.

> Roughly speaking, a function is referentially transparent if it does not execute any side effects.

Callback on futures have on very useful property. Programs using only the Future.apply and foreach calls with referentially transparent callbacks are 
deterministic. For the same input, such programs will always compute the same result.

> Programs composed from referentially transparent future computations and callbacks are deterministic.

<br/>

### Futures and Exceptions

When the future is completed, it is either completed successfully or has failed. After that, the future's state no longer 
changes, and registering a callback immediately **schedule (not execute)** it for execution.

```scala
object FuturesTry extends App with ThreadUtils with ExecutorUtils {
  val threadName: Try[String] = Try(Thread.currentThread.getName)
  val someText: Try[String] = Try("Try objects are synchronous")
  val message: Try[String] = for {
    tn <- threadName
    st <- someText
  } yield s"Message $st was create on t = $tn"

  def handleMessage(t: Try[String]) = t match {
    case Success(msg) => log(msg)
    case Failure(error) => log(s"unexpected failure - $error")
  }

  handleMessage(message)
}
```

<br/>

### Fatal Exceptions

`InterruptedException` and some severe program error such as

- `LinkageError`
- `VirtualMachineError`
- `ThreadDeath`
- `ControlThrowable`

are forwarded to the execution context's reportFailure method. These types of `Throwable` objects are 
called **fatal errors**. 

You can pattern match non-fatal exceptions using `NonFatal`

```scala
g.failed foreach { case NonFatal(t) => log(s"error $t")}
```

[SO: NonFatal vs Exception](http://stackoverflow.com/questions/29744462/the-difference-between-nonfatal-and-exception-in-scala)

```scala
object NonFatal {
   def apply(t: Throwable): Boolean = t match {
     // VirtualMachineError includes OutOfMemoryError and other fatal errors
     case _: VirtualMachineError | _: ThreadDeath | _: InterruptedException | _: LinkageError | _: ControlThrowable => false
     case _ => true
   }
   
  def unapply(t: Throwable): Option[Throwable] = if (apply(t)) Some(t) else None
}
```

> Future computations do not catch fatal errors. Use `NonFatal` extractor to pattern match against nonfatal errors.

<br/>

## Functional Composition on Futures
 
> There is a happens-before relationship between completing the future and invoking the function in any of its combinators.
 
While the future resulting from a `map` method completes when the mapping function `f` completes, 
the future resulting from a `flatMap` method completes when both `f` and the future returned by `f` complete.
 
The `flatMap` combinator combines two futures into one: the one on which `flatMap` is invoked and the one 
that is returned by the argument function.

## Promises

```scala
object PromisesCreate extends App with ThreadUtils {
  val p = Promise[String]
  val q = Promise[String]

  p.future foreach { case x => log(s"p succeeded with $x") }
  q.future.failed foreach { case t => log(s"q failed with $t")}

  Thread.sleep(1000)

  p.success("assigned")
  q.failure(new Exception("not kept"))

  Thread.sleep(1000);
}
```

Assigning a value or an exception to an already completed promise is not allowed and throws an exception.

Using `Future.apply` and callback method with referentially transparent functions results in deterministic concurrent programs. 
As long as we do not use the `trySuccess`, `tryFailure`, and `tryComplete` methods, and none of the `success`, `failure`, and `complete` methods 
ever throws an exception, we can use promises and retain determinism in our programs.

## Converting callback-based APIs

> Use promises to bridge the gap between callback-based APIs and futures

```scala
object FileSystemMonitor {
  def fileCreated(directory: String): Future[String] = {
    val p = Promise[String]

    val fileMonitor = new FileAlterationMonitor(1000)
    val observer = new FileAlterationObserver(directory)
    val listener = new FileAlterationListenerAdaptor {
      override def onFileCreate(file: File): Unit = {
        try p.trySuccess(file.getName) finally  fileMonitor.stop()
      }
    }

    observer.addListener(listener)
    fileMonitor.addObserver(observer)
    fileMonitor.start()

    p.future
  }
}

object FileSystemMonitorExample extends App with ThreadUtils {
  import scala.concurrent.ExecutionContext.Implicits.global

  FileSystemMonitor.fileCreated(".") foreach {
    case filename => log(s"Detected new file $filename`")
  }
}
```

We can also use `Timer` for example, 

```scala
object FutureUtils {
  private val timer = new Timer(true)

  def timeout(t: Long): Future[Unit] = {
    val p = Promise[Unit] 
    
    timer.schedule(new TimerTask {
      override def run(): Unit = {
        p success ()
        timer.cancel()
      }  
    }, t)
    
    p.future
  }  
}
```

## Extending the Future API

If you call a non-existing `xyz` method on an object of some `A` type, the Scala compiler will search for all implicit conversions from 
the `A` type to some other type that has `xyz` method. One way to define such an implicit conversion is to use **implicit classes**

```scala
package object future {
  implicit class FutureOps[T](val self: Future[T]) {
    def or(that: Future[T]): Future[T] = {
      val p = Promise[T]

      self onComplete { case x => p tryComplete x }
      that onComplete { case y => p tryComplete y }

      p.future
    }
  }
}
```

## Cancellation of asynchronous computations

```scala
object PromisesCancellation extends App with ThreadUtils {
  type Cancellable[T] = (Promise[Unit], Future[T])

  def cancellable[T](b: Future[Unit] => T): Cancellable[T] = {
    val cancel = Promise[Unit]

    val f = Future {
      var r: T = b(cancel.future)
      if (!cancel.tryFailure(new Exception))
        throw new CancellationException

      r
    }

    (cancel, f)
  }
  
  val (cancel, value) = cancellable( cancle => {
    var i = 0
    
    while (i < 5) {
      if (cancle.isCompleted) throw new CancellationException
      
      Thread.sleep(500)
      log(s"$i: working")
      i += i
    }
    
    "resulting values"
  })
  
  Thread.sleep(1500)
  cancel trySuccess ()

  log("computation cancelled")

  Thread.sleep(2000)
}
```

Note that calling `trySuccess` on the cancel promise does not guarantee that the computation will 
really be cancelled. It is entirely possible that the asynchronous computation fails the `cancel` promise 
before the client has a change to cancel it. Thus, the client, such as the main thread in our example, should in general use 
the return value from `trySuccess` to check if the cancellation succeeded.

## Blocking in asynchronous computations

> Avoid blocking in asynchronous computations,as it can cause thread starvation.

```scala
object FutureBlockingExample1 extends App with ThreadUtils {
  val startTime = System.nanoTime

  val futures = for (_ <- 0 until 16) yield Future {
    Thread.sleep(1000)
  }

  for (f <- futures) Await.ready(f, Duration.Inf)

  val endTime = System.nanoTime

  log(s"Total time = ${(endTime - startTime) / 1000000}")
  log(s"Total CPUs = ${Runtime.getRuntime.availableProcessors}")
}

// output
> runMain future.FutureBlockingExample1
[info] Running future.FutureBlockingExample1 
[info] main: Total time = 2105
[info] main: Total CPUs = 8
```

If you absolutely must block, then the part of the code that blocks should be enclosed within the `blocking` call. 
This signals to the execution context that the worker thread is blocked and allows it to temporarily spawn additional 
worker threads if necessary.

```scala
object FutureBlockingExample2 extends App with ThreadUtils {
  val startTime = System.nanoTime

  val futures = for (_ <- 0 until 16) yield Future {
    blocking { Thread.sleep(1000) }
  }

  for (f <- futures) Await.ready(f, Duration.Inf)

  val endTime = System.nanoTime

  log(s"Total time = ${(endTime - startTime) / 1000000}")
  log(s"Total CPUs = ${Runtime.getRuntime.availableProcessors}")
}

// output
[info] Running future.FutureBlockingExample2 
[info] main: Total time = 1099
[info] main: Total CPUs = 8
```

With the `blocking` call around the `sleep` call, the `global` execution context spawns additional thread when it 
detects that there is more work than the worker threads. All 16 future computations can execute concurrently, and the 
program terminates after one second.

> The `Await.ready` and `Await.result` statements block the caller thread until the future is completed, and, are is most cases used outside asynchronous computations. 
They are blocking operations. The `blocking` statement is used inside asynchronous code to designate that the enclosed block of code contains a blocking call. 
It is not a blocking operation by itself.

## The Scala Async Library

> Scala Async Library allows writing shorter, more concise, and understandable programs.

The Scala Async library introduced two new method calls. `async`, `await`. The `async` method is conceptually equivalent to the `Future.apply` method. 
The `await` method takes a future future and returns that future's value. However, unlike the 
methods on the `Await` object, the `await` method does not block the underlying thread. 

The `await` method must be statically enclosed with in an `async` block in the same method.

```scala
object AsyncExample1 extends App with ThreadUtils {
  def delay(second: Int): Future[Unit] = async {
    blocking { Thread.sleep(second * 1000)}
  }

  async {
    log("T-minus 1 second")
    await { delay(1) }
    log("done")
  }

  Thread.sleep(2000)
}
```

How can the Scala async library execute the preceding example without blocking? The answer is that the 
Async library uses Scala Macros to transform the code inside the `async` statement.
 
The code is transformed in such a way that the code after every `await` statement becomes a callback 
registered to the future inside await.

```scala
Future {
  log("T-minus 1 second")
  delay(1) foreach {
   case _ => log("done") 
  }
}
```

The equivalent code produced by the Scala Async library is completely non-blocking.

## Alternative Future Frameworks


















