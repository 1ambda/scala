package chapter7

import java.util.concurrent.{Callable, TimeUnit, Future, ExecutorService}


object Par {
  type Par[A] = ExecutorService => Future[A]

  case class UnitFuture[A](get: A) extends Future[A] {
    def isDone = true
    def get(timeout: Long, timeUnit: TimeUnit) = get
    def isCancelled = false
    def cancel(evenIfRunning: Boolean): Boolean = true
  }

  def map2[A, B, C](pa: Par[A], pb: Par[B])(f: (A, B) => C): Par[C] =
    (es: ExecutorService) => {
      val fa = pa(es)
      val fb = pb(es)
      UnitFuture(f(fa.get, fb.get))
    }

  def unit[A](a: A): Par[A] = (es: ExecutorService) => UnitFuture(a)

  def fork[A](a: => Par[A]): Par[A] =
    (es: ExecutorService) => es.submit(new Callable[A] {
      override def call(): A = a(es).get
    })

  def lazyUnit[A](a: => A): Par[A] = fork(unit(a))
  def run[A](es: ExecutorService)(a: Par[A]): Future[A] = a(es)

  def asyncF[A, B](f: A => B): A => Par[B] =
    (a: A) => lazyUnit(f(a))

  def _sortPar(parList: Par[List[Int]]): Par[List[Int]] =
    map2(parList, unit(()))((xs, _) => xs.sorted)

  def map[A, B](pa: Par[A])(f: A => B): Par[B] =
    map2(pa, unit(()))((a, _) => f(a))

  def sortPar(parList: Par[List[Int]]): Par[List[Int]] =
    map(parList)(_.sorted)

  def parMap[A, B](xs: List[A])(f: A => B): Par[List[B]] = {
    val ps: List[Par[B]] = xs.map(asyncF(f))
    sequence(ps)
  }

  def sequence[A](ps: List[Par[A]]): Par[List[A]] =
    ps.foldRight[Par[List[A]]](unit(List()))((x, acc) => map2(x, acc)(_ :: _))

  def parFilter[A](xs: List[A])(f: A => Boolean): Par[List[A]] = {
//    val ps: Par[List[A]] = parMap(xs)(x => x)
//    map(ps)(xs => xs.filter(f))

    // ref: https://github.com/fpinscala/fpinscala/blob/master/answers/src/main/scala/fpinscala/parallelism/Par.scala
    var pars: List[Par[List[A]]] = xs map ( asyncF(a => if (f(a)) List(a) else Nil) )
    map(sequence(pars))(_.flatten)
  }

  def delay[A](pa: => Par[A]): Par[A] = es => pa(es)
}
