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


