# Reactive Extension

## Rx Contract

Every `Observable` object can call `onNext` on its `Observer` objects zero or more times. An `Observable` object might 
then enter the completed or error state by calling `onCompleted` or `onError` on its `Observer` objects. This is known as the **Observable contract**.

## Implementing Custom Observable Objects

```scala
def create(f: Observer[T] => Subscription): Observable[T]
```

Whenever the `subscribe` method gets called, the function `f` is called on the corresponding `Observer` object. 
The function `f` returns a `Subscription` object, which can be used to unsubscribe the `Observer` object from the `Observable` instance.

```scala
object FileSystemMonitor {
  def modified(directory: String): Observable[String] = {
    Observable.create { observer => 
      
      val fileMonitor = new FileAlterationMonitor(1000)
      val fileObs = new FileAlterationObserver(directory)
      val fileLis = new FileAlterationListenerAdaptor {
        override def onFileChange(file: java.io.File): Unit = {
          observer.onNext(file.getName)
        }
      }
      
      fileObs.addListener(fileLis)
      fileMonitor.addObserver(fileObs)
      fileMonitor.start()
      
      Subscription { fileMonitor.stop() }
    }
  }
}
```

Calling `unsubscribe` the second time will not run the specified block of code again. We say that the `unsubscribe` method is **idempotent**. Calling it multiple times has 
the same effect as calling it only once. In our example, the `unsubscribe` method calls the `stop` method of the `fileMonitor` object at most once.

When subclassing the `Subscription` trait, we need to ensure that `unsubscrie` is idempotent, and the `Subscription.apply` method is a convenience method that ensures idempotence automatically.

<br/>

## Concat, Flatten, FlatMap

> Use `concat` to flatten nested `Observable` object whenever the order of events to be maintained

> If at least one of the nested `Observable` object has unbounded number of events or never completes, use `flatten` instead of `concat`

<br/>

## Cold Observable Example

```scala
def randomQuote = Observable.create[String] { (obs: Observer[String]) =>
  val url = "http://www.iheartquotes.com/api/v1/random?" +
    "show_permalink=false&show_source=false"
  obs.onNext(Source.fromURL(url).getLines.mkString)
  obs.onCompleted()
  Subscription()
}
```

`randomQuote` fetches a new quote every time `subscribe` is called.

## Failure Handling in Observables 

```scala
object CompositionScan extends App with ThreadUtils {
  CompositionRetry.quoteMessage.retry.repeat.take(100).scan(0) {
    (n, q) => if (q == "Retrying...") n + 1 else n
  } subscribe (n => log(s"$n / 100"))
}
```

The `retry` method is used in order to repeat the events from failed `Observable` objects. Similarly, 
the `repeat` method is used in order to repeat the events from completed `Observable` objects.

```scala
object CompositionErrors extends App with ThreadUtils {
  val status = items("ok", "still ok") ++ error(new Exception)
  
  val fixedStatus = status.onErrorReturn(e => "exception occurred")
  fixedStatus.subscribe(log _)
  
  val continuedStatus = status.onErrorResumeNext(e => items("better", "much better"))
  continuedStatus.subscribe(log _)
}
```

## Subject

```scala
object RxOS extends ThreadUtils {
  val messageBus = Observable.just(
    TimeModule.systemClock,
    FileSystemModule.fileModifications
  ).flatten.subscribe(log _)
}

object FileSystemModule {
  val fileModifications = FileSystemMonitor.modified(".")
}

object TimeModule {
  import Observable._
  import scala.concurrent.duration._

  val systemClock = interval(1 second).map(t => s"system time $t")
}
```

If another kernel module introduces another group of important system events, we will have to 
recompile he RxOS kernel each time some third-party developer implements a kernel modules.

This is the classic culprit of the bottom up programming style. We are unable to declare the `messageBus` object without declaring 
its dependencies.

```scala
// top-down

object RxOS extends ThreadUtils {
  val messageBus = Subject[String]()
  val messageLog = subjects.ReplaySubject[String]()
  messageBus.subscribe(log _)
  messageBus.subscribe(messageLog)
}

object RxOSRuntime extends App with ThreadUtils {
  log(s"RxOS botting...")

  val loadedModules = List(
    TimeModule.systemClock,
    FileSystemModule.fileModifications
  ).map(_.subscribe(RxOS.messageBus))

  log(s"RxOS booted")

  Thread.sleep(1000)

  for (mod <- loadedModules) mod.unsubscribe()
  log(s"RxOS going for shutdown")
}
```

