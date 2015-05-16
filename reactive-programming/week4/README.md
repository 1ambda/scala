# Week4, Oberservable

### Connectable Observable

```scala
// ref: http://reactivex.io/documentation/operators.html#connectable
// ref: http://leecampbell.blogspot.kr/2010/08/rx-part-7-hot-and-cold-observables.html
test("ConnectableObservable Exploit2") {
  val hot = Observable.interval(200 millis).publish
  hot.connect

  Thread.sleep(500)

  val r = hot.replay
  r.connect

  r.subscribe(x => println(s"Observer1 : $x"))
  Thread.sleep(200)
  r.subscribe(x => println(s"Observer2 : $x"))

  hot.take(600 millis).toBlocking.toList
}

// result
Observer1 : 0
Observer1 : 1
Observer1 : 2
Observer2 : 0
Observer2 : 1
Observer2 : 2
Observer1 : 3
Observer2 : 3
Observer1 : 4
Observer2 : 4
Observer1 : 5
Observer2 : 5
```

### From Try to Future

[Future and Try are not dual](http://cstheory.stackexchange.com/questions/20117/in-what-sense-are-scalas-tryt-and-futuret-dual)

```scala
trait Future[T] {
  def onComplete[U](f: Try[T] => U)(implicit ex: ExecutionContext): Unit
}
```

So, simplify Future as `(Try[T] => Unit) => Unit`. Then flip the type. As a result, we can see `Try[T]`

```scala
(Try[T] => Unit) => Unit

// flip
Unit => (Unit => Try[T])

// simplify
() => (() => Try[T])

// simplify more
Try[T]
```

### From iterables to Observables

```scala
trait Iterable[T] { def iterator(): Iterator[T] }
trait Iterator[T] ( def hasNext: Boolean; def next(): T }
```

`Iterable` is also a monad

Flip and simplify as we did above. then,

```scala
trait Iterator[T] ( def hasNext: Boolean; def next(): T }

// remove hasNext
trait Iterator[T] ( def next(): Option[T] }

// remove next
type Iterator[T] = () => Option[T]

// replace the type of `Iterator` as we defined before 
trait Iterable[T] { def iterator() : () => Option[T] }

// make `Iterator` a single type 
type Iterable[T] = () => (() => Try[Option[T]])
 
// flip the arrow!
(Try[Option[T]] => ()) => ()

// same as
(Try[Option[T]] => Unit) => Unit
```

This is a `Observable` !

```scala
type Observable[T] = (Try[Option[T]] => Unit) => Unit

// same as
type Observable[T] = (Throwable=>Unit, ()=>Unit, T=>Unit) => Unit
```

Now, we can define `Observer`, `Subscription`

```scala
trait Observer[T] = {
  def subscribe(o: Observer[T]: Subscription
}

trait Observer[T] = {
  def onError(t: Throwable): Unit
  def onCompleted(): Unit
  def onNext(value: T): Unit
}

trait Subscription {
  def unsubscribe(): Unit
  def isUnsubscribed: Boolean
}
```

### Hello, Observable

```scala
def waitFor[T](obs: Observable[T]): Unit = {
    obs.toBlocking.toIterable.last
}

test("slidingBuffer example") {
    val ticks = Observable.interval(100 millis)
    val evens = ticks.filter(_ % 2 == 0)
    val bufs = evens.slidingBuffer(count = 2, skip = 3)
    val obs = bufs.take(4)
    
    obs.subscribe(println(_))
    waitFor(obs)
}
```

### Rx Operators

[Ref: http://reactivex.io/RxJava/javadoc/rx/Observable.html](http://reactivex.io/RxJava/javadoc/rx/Observable.html)

#### flatMap

![](https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flatMap.png)

```scala
def flatMap(f: T => Observable[S]): Observable[S] = { map(f).flatten }

// ref: https://github.com/ReactiveX/RxScala/blob/0.x/src/main/scala/rx/lang/scala/Observable.scala#L945
// actually 
def flatMap[R](f: T => Observable[R]): Observable[R] = {
  toScalaObservable[R](asJavaObservable.flatMap[R](new Func1[T, rx.Observable[_ <: R]]{
    def call(t1: T): rx.Observable[_ <: R] = { f(t1).asJavaObservable }
  }))
}
```

#### concat, flatten

![](https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concat.png)

![](https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/merge.png)

#### groupBy

![](https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/groupBy.png)

```scala
val withCountry: Observable[Observable[(EarthQuake, Country)]] = {
  usgs().map(quake => {
    val country: Future[Country] = reverseGeocode(quake.location)
    Observable.from(country.map(c => (quake, c)))
  })
}

val m: Observable[(EarthQuake, Country)] = withCountry.flatten
val c: Observable[(EarthQuake, Country)] = withCountry.concat // ordered. but slow.

val byCountry: Observable[(Country, Observable[(EarthQuake, Country)])] = 
  m.groupBy({case (quake, country) => country })
```

<br/>

### Subscription

#### Hot, Cold Observable

[Article: Hot and Col Observables](http://davesexton.com/blog/post/Hot-and-Cold-Observables.aspx)

![](http://cdn.liginc.co.jp/wp-content/uploads/2015/03/cool_vs_hot.jpg)
(ref: http://liginc.co.jp/web/js/151272)

- **Cold Observable:** Each subscriber has its own private source. (subscription causes side effect)

- **Hot Observable:** Same source shared by all subscribers (subscription has no side effect) e.g. UIEvent

> Unsubscribing is not Cancellation!. There might be other observers.


#### Kind of Subscriptions

```scala
// real implementation
// https://github.com/ReactiveX/RxScala/tree/0.x/src/main/scala/rx/lang/scala/subscriptions


// Collection of subscriptions
class CompositeSubscription extends Subscription {
  def +=(s: Subscription): this.type
  def -=(s: Subscription): this.type
}


// Swap underlying subscription
class MultiAssignmentSubscription extends Subscription {
  def subscription: Subscription
  def subscription-=(that: Subscription): this.type
}


// unsubscribe when swapped
class SerialSubscription extends Subscription {
  def subscription: Subscription
  def subscription-=(that: Subscription): this.type
}
```

```scala
object fixture {
  def a = Subscription { println("A") }
  def b = Subscription { println("B") }
  def c = Subscription { println("C") }
}

test ("SerialSubscription") {
  val a = fixture.a
  val b = fixture.b
  val c = fixture.c

  val serial: SerialSubscription = SerialSubscription()
  serial.subscription = a

  a.isUnsubscribed should be (false)
  serial.subscription = b
  a.isUnsubscribed should be (true)
}

test("MultiAssignment") {
  val a = fixture.a
  val b = fixture.b
  val c = fixture.c

  val multi = MultipleAssignmentSubscription()

  multi.isUnsubscribed should be (false)

  multi.subscription = a
  multi.subscription = b

  multi.unsubscribe()

  a.isUnsubscribed should be (false)
  b.isUnsubscribed should be (true)

  // already unsubscribed
  multi.subscription = c
  multi.isUnsubscribed should be (true)
}


ignore ("unsubscribe CompositeSubscription") {

  val a = fixture.a
  val b = fixture.b

  val composite = CompositeSubscription(a, b)

  composite.isUnsubscribed should be (false)

  composite.unsubscribe()

  composite.isUnsubscribed should be (true)
  a.isUnsubscribed should be (true)
  b.isUnsubscribed should be (true)

  val c = fixture.c

  composite += c
  c.isUnsubscribed should be (true)
}
```

### Promise and Subjects

```scala
// map impl on `Future`

def map[S](f: T => S)(implicit executor: ExecutionContext): Future[S] = {
  val p = Promise[S]()
  
  this.onComplete {
    case t => try { p.success(f(t)) }
              catch { case e => p.failure(e) }
  }
  
  p.future
}
```

> `Subject` is the mutable variable of Rx. [Ref: Subject. Are they to be avoided?](http://stackoverflow.com/questions/9299813/rx-subjects-are-they-to-be-avoided)

```scala
// real impl
// https://github.com/ReactiveX/RxScala/blob/0.x/src/main/scala/rx/lang/scala/Subject.scala

trait Subject[T] extends Observable[T] with Obserber[T] {
  override def onNext(value: T): Unit
  override def onError(error: Throwable): Unit
  override def onCompleted()
  
  def hasObservers: Boolean
  
  ...
  ...
}
```

Subjects are like channels

[Ref: ReactiveX - Subject](http://reactivex.io/documentation/subject.html)

- **AsyncSubject:** emits the last value
- **BehaviorSubject:** begins by emitting the item most recently emitted
- **PublishSubject:** emits items which passed after the subscription
- **ReplaySubject:** emit all the items were emitted by the source regardless of when the observer subscribes


![](http://reactivex.io/documentation/operators/images/S.AsyncSubject.png)

![](http://reactivex.io/documentation/operators/images/S.BehaviorSubject.png

![](http://reactivex.io/documentation/operators/images/S.PublishSubject.png)

![](http://reactivex.io/documentation/operators/images/S.ReplaySubject.png)

```scala
class SubjectSpec extends FunSuite with Matchers {

  def record(s: String): Int => Unit = (x) => println(s"$s: $x")

  test("behavior subject") {
    val channel = BehaviorSubject[Int]()

    val a = channel.subscribe(record("a"))

    channel.onNext(31)
    channel.onNext(32)

    val b = channel.subscribe(record("b"))

    // a: 31, 32 b: 32
    channel.onCompleted()
  }

  ignore ("replay subject") {
    val channel = ReplaySubject[Int]()

    val a = channel.subscribe(record("a"))

    channel.onNext(31)
    channel.onNext(32)

    channel.onCompleted()

    // a: 31, 32 b: 31, 32
    val b = channel.subscribe(record("b"))

  }

  ignore ("async subject") {
    val channel = AsyncSubject[Int]()

    val a = channel.subscribe(record("a"))
    channel.onNext(37)
    channel.onNext(38)

    // values passed into onNext will not emit until calling onCompleted
    // a:38, b:38.

    // AsyncSubject caches final value
    channel.onCompleted()
    val b = channel.subscribe(record("b"))
  }

  ignore ("publish subject") {
    val channel = PublishSubject[Int]()

    val a = channel.subscribe(record("a"))
    val b = channel.subscribe(record("b"))

    channel.onNext(42)

    a.unsubscribe()

    channel.onNext(4711)

    // has no effect on subscriptions in the channel even we add new subs
    channel.onCompleted()
    channel.onNext(13)
    val c = channel.subscribe(record("c"))

    a.isUnsubscribed should be (true)
    c.isUnsubscribed should be (false) // not unsubscribed yet.
  }

}
```

### Rx Potpourri

#### Converting Future to Observable

```scala
object Observable {

  def apply[T](f: Future[T]): Observable[T] = {
    val s = AsyncSubject[T]()
      
    f.onComplete {
      case Failure(t) => s.onError(t)
      case Success(v) =>  s.onNext(v); s.onCompleted()
    }
    
    s
  }
}
```

#### Observable notifications

[Ref: ReactiveX docs](http://reactivex.io/documentation/operators/materialize-dematerialize.html)

![](http://reactivex.io/documentation/operators/images/materialize.c.png)

![](http://reactivex.io/documentation/operators/images/dematerialize.c.png)

```scala
abstract class Try[+T]
case class Success[T](elem: T) extends Try[T]
case class Failure(t: Throwable) extends Try[Nothing]

abstract class Notification[+T]
case class OnNext[T](elem: T) extends Notification[T]
case class OnError(t: Throwable) extends Notification[Nothing]
case object OnCompleted extends Notification[Nothing]

def materialize: Observable[Notification[T]] = { ... }
```

#### Reduce

![](http://reactivex.io/documentation/operators/images/reduceSeed.png)

<br/>

### Observable Contract

- Never, ever implement `Observerable[T]` or `Observer[T]` yourself.
- Always use the factory methods.

Since, they have complicated implementation to support auto-subscribing  

- There is a [Rx design guide](https://www.google.com/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&ved=0CCAQFjAA&url=http%3A%2F%2Fgo.microsoft.com%2Ffwlink%2F%3FLinkID%3D205219&ei=4CBPVffmCsz08QX31IGYDw&usg=AFQjCNGTJedImoPCXNzUMlwgpuyEVU1rCA&sig2=jgax6lhM7eqrfw02UDBymw&bvm=bv.92885102,d.dGc&cad=rjt). 
