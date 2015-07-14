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
 
 





