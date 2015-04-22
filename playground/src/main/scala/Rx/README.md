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

