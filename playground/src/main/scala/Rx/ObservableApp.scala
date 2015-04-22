package Rx

import rx.lang.scala.Observable
import rx.lang.scala.Subscription

// ref: https://github.com/ReactiveX/RxScala/blob/0.x/examples/src/main/scala/
object ObservableApp extends App {

  def hello(names: String*) {
    val observer = { name: String =>
      println(s"Hello, $name!")
    }

    Observable.from(names) subscribe observer
    Observable.just("a", "b", "c") subscribe observer
  }

  hello("1ambda", "2ambda", "3ambda")


}
