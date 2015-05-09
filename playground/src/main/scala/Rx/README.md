## Thread Experiments

[Refs: http://www.grahamlea.com/2014/07/rxjava-threading-examples/](http://www.grahamlea.com/2014/07/rxjava-threading-examples/)

```scala
// package object
import rx.lang.scala._

package object Rx {

  implicit class ObservableOps[T](o: Observable[T]) {
    def debug(message: String) = {
      o.doOnNext(v => Rx.debug(message, v))
    }
  }

  def debug[T](message: String, value: T): Unit = {
    println(s"[${Thread.currentThread().getName}] ${message}: ${value}")
  }
}

// test case
@RunWith(classOf[JUnitRunner])
class ObservableSpec extends FunSuite with Matchers {

  def range(b: Int) = (b to b + 4).toList.map(_.toString)
  def generator(base: Int) = Observable.from(range(base)).debug("Generated")

  def action(a: String): (String => String) = (value) => value + a
  def plus = action("+")
  def minus = action("-")

  val inc = generator(1).map(plus).debug("plus")
  val dec = inc.map(minus).debug("minus")
  
  ...
  ...
```

### subscribeOn

![](http://reactivex.io/documentation/operators/images/subscribeOn.c.png)

`subscribeOn` specify the `Scheduler` on which an `Observable` will operate

The `ObserveOn` operator is similar, but more limited. It instructs the `Observable` to
 send notifications to observers on a specified `Scheduler`
 
즉, `Observable` 에게 어느 스레드에서 돌고있는 `Observer` 에게 전송할지를 결정하는게 `observeOn`

```scala
test ("run on IoScheduler") {
    generator(1).subscribeOn(IOScheduler()).subscribe(x => x)
    generator(11).subscribeOn(IOScheduler()).subscribe(x => x)
}

// result
[RxCachedThreadScheduler-1] Generated: 1
[RxCachedThreadScheduler-1] Generated: 2
[RxCachedThreadScheduler-1] Generated: 3
[RxCachedThreadScheduler-2] Generated: 11
[RxCachedThreadScheduler-1] Generated: 4
[RxCachedThreadScheduler-2] Generated: 12
[RxCachedThreadScheduler-1] Generated: 5
[RxCachedThreadScheduler-2] Generated: 13
[RxCachedThreadScheduler-2] Generated: 14
[RxCachedThreadScheduler-2] Generated: 15
```

<br/>

### observeOn

![](http://reactivex.io/documentation/operators/images/observeOn.c.png)

Specify the `Scheduler` on which an `Observer` will observe this `Observable`

```scala
test ("observeOn") {
  generator(1).subscribeOn(IOScheduler()).observeOn(ComputationScheduler())
    .map(plus).debug("plus").subscribe(x => x)
}

// result
[RxCachedThreadScheduler-1] Generated: 1
[RxCachedThreadScheduler-1] Generated: 2
[RxCachedThreadScheduler-1] Generated: 3
[RxCachedThreadScheduler-1] Generated: 4
[RxCachedThreadScheduler-1] Generated: 5
[RxComputationThreadPool-3] plus: 1+
[RxComputationThreadPool-3] plus: 2+
[RxComputationThreadPool-3] plus: 3+
[RxComputationThreadPool-3] plus: 4+
[RxComputationThreadPool-3] plus: 5+
```

<br/>

### observeOn vs subscribeOn

```java
// ref: https://github.com/ReactiveX/RxAndroid
public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Observable.from("one", "two", "three", "four", "five")
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(/* an Observer */);
}
```

The above code will execute an observer which passed as an argument on a new thread, and emit
 result through `onNext` on the main UI thread.
 
In Scala,

```scala
test ("observeOn + subscribeOn") {
  val inc = generator(1).subscribeOn(ComputationScheduler()).map(plus).debug("plus")
  val dec = inc.observeOn(IOScheduler()).map(minus).debug("minus")

  dec.subscribe(x => x)
}

// result
[RxComputationThreadPool-3] Generated: 1
[RxComputationThreadPool-3] plus: 1+
[RxComputationThreadPool-3] Generated: 2
[RxComputationThreadPool-3] plus: 2+
[RxComputationThreadPool-3] Generated: 3
[RxComputationThreadPool-3] plus: 3+
[RxComputationThreadPool-3] Generated: 4
[RxCachedThreadScheduler-1] minus: 1+-
[RxComputationThreadPool-3] plus: 4+
[RxCachedThreadScheduler-1] minus: 2+-
[RxComputationThreadPool-3] Generated: 5
[RxCachedThreadScheduler-1] minus: 3+-
[RxComputationThreadPool-3] plus: 5+
[RxCachedThreadScheduler-1] minus: 4+-
[RxCachedThreadScheduler-1] minus: 5+-
```

`observeOn` + `observeOn`

```scala
test ("observeOn + observeOn") {
  val inc = generator(1).subscribeOn(IOScheduler()).map(plus).debug("plus")
  val dec = inc.observeOn(IOScheduler()).map(minus).debug("minus")

  dec.subscribe(x => x)
}

// result
[RxCachedThreadScheduler-2] Generated: 1
[RxCachedThreadScheduler-2] plus: 1+
[RxCachedThreadScheduler-2] Generated: 2
[RxCachedThreadScheduler-2] plus: 2+
[RxCachedThreadScheduler-2] Generated: 3
[RxCachedThreadScheduler-2] plus: 3+
[RxCachedThreadScheduler-2] Generated: 4
[RxCachedThreadScheduler-2] plus: 4+
[RxCachedThreadScheduler-1] minus: 1+-
[RxCachedThreadScheduler-2] Generated: 5
[RxCachedThreadScheduler-1] minus: 2+-
[RxCachedThreadScheduler-2] plus: 5+
[RxCachedThreadScheduler-1] minus: 3+-
[RxCachedThreadScheduler-1] minus: 4+-
[RxCachedThreadScheduler-1] minus: 5+-
```

### delated

Some RxScala operators use threading implicitly. for example, `delay` uses `ComputingScheduler`

```scala
test("delay function use `subscribeOn`") {
  val inc = generator(1).map(plus).debug("plus")
  val delayed = inc.delay(Duration(1, MILLISECONDS)).debug("delayed")
  val dec = delayed.map(minus).debug("minus")

  dec.subscribe(x => x)
}

// result
[ScalaTest-run-running-ObservableSpec] Generated: 1
[ScalaTest-run-running-ObservableSpec] plus: 1+
[ScalaTest-run-running-ObservableSpec] Generated: 2
[ScalaTest-run-running-ObservableSpec] plus: 2+
[ScalaTest-run-running-ObservableSpec] Generated: 3
[ScalaTest-run-running-ObservableSpec] plus: 3+
[ScalaTest-run-running-ObservableSpec] Generated: 4
[ScalaTest-run-running-ObservableSpec] plus: 4+
[ScalaTest-run-running-ObservableSpec] Generated: 5
[ScalaTest-run-running-ObservableSpec] plus: 5+
[RxComputationThreadPool-1] delayed: 1+
[RxComputationThreadPool-1] minus: 1+-
[RxComputationThreadPool-1] delayed: 2+
[RxComputationThreadPool-1] minus: 2+-
[RxComputationThreadPool-1] delayed: 3+
[RxComputationThreadPool-1] minus: 3+-
[RxComputationThreadPool-1] delayed: 4+
[RxComputationThreadPool-1] minus: 4+-
[RxComputationThreadPool-1] delayed: 5+
[RxComputationThreadPool-1] minus: 5+-
```


## Observable

[Ref: ReactiveX observable](http://reactivex.io/documentation/observable.html)

![](http://reactivex.io/assets/operators/legend.png) 


An **Observer** subscribes to an **Observable**

```scala
val names: String*

Observable.from(names) subscribe { name =>
  println(s"Hello, $name!")
}

// or
val observer = { name: String =>
  println(s"Hello, $name!")
}

Observable.from(names) subscribe observer 
```

- `subscribe` return a `Subscription` [see RxScala: Observable.scala](https://github.com/ReactiveX/RxScala/blob/0.x/src/main/scala/rx/lang/scala/Observable.scala#L145)

- `Observer` has factory methods. [Observer.scala](https://github.com/ReactiveX/RxScala/blob/0.x/src/main/scala/rx/lang/scala/Observer.scala#L89)

```scala
// in Observable
def subscribe(o: Observer[T]): Subscription

// in Observer object
object Observer {
  def apply[T](onNext: T => Unit)
}
```

