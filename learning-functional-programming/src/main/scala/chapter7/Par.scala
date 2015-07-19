package chapter7

import java.util.concurrent.{Callable, TimeUnit, Future, ExecutorService}


object Par {

  type Par[A] = ExecutorService => Future[A]
  case class UnitFuture[A](get: A) extends Future[A] {
    def isDone = true
    def get(timeout: Long, timeunit: TimeUnit) = get
    def isCancelled = false
    def cancel(evenIfRunning: Boolean): Boolean = true
  }

  def map2[A, B, C](pa: Par[A], pb: Par[B])(f: (A, B) => C): Par[C] =
    (es: ExecutorService) => {
      val af = pa(es)
      val bf = pb(es)
      UnitFuture(f(af.get, bf.get))
    }

  def unit[A](a: A): Par[A] = (es: ExecutorService) => UnitFuture(a)

  def fork[A](a: => Par[A]): Par[A] =
    (es: ExecutorService) => es.submit(new Callable[A] {
      override def call(): A = a(es).get
    })
  
  def lazyUnit[A](a: => A): Par[A] = fork(unit(a))
  def run[A](es: ExecutorService)(a: Par[A]): Future[A] = a(es)



}
