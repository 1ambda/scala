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
