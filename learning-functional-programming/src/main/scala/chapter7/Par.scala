package chapter7

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.{CountDownLatch, Callable, TimeUnit, ExecutorService}


object Par {
  sealed trait Future[A] {
    private[chapter7] def apply(callback: A => Unit): Unit
  }

  type Par[A] = ExecutorService => Future[A]

  def map2[A, B, C](pa: Par[A], pb: Par[B])(f: (A, B) => C): Par[C] =
    (es: ExecutorService) => new Future[C] {
      def apply(callback: (C) => Unit): Unit = {
        var ar: Option[A] = None
        var br: Option[B] = None

        val combiner = Actor[Either[A, B]](es) {
          case Left(a) => br match {
            case None => ar = Some(a)
            case Some(b) => eval(es)(callback(f(a, b)))
          }

          case Right(b) => ar match {
            case None => br = Some(b)
            case Some(a) => eval(es)(callback(f(a, b)))
          }
        }

        pa(es)(a => combiner ! Left(a))
        pb(es)(b => combiner ! Right(b))
      }
    }

  def unit[A](a: A): Par[A] =
    (es: ExecutorService) => new Future[A] {
      def apply(callback: (A) => Unit): Unit =
        callback(a)
    }

  def fork[A](a: => Par[A]): Par[A] =
    (es: ExecutorService) => new Future[A] {
      def apply(callback: (A) => Unit): Unit =
        eval(es)(a(es)(callback))
    }

  def eval(es: ExecutorService)(r: => Unit): Unit =
    es.submit(new Callable[Unit] { def call = r })

  def lazyUnit[A](a: => A): Par[A] = fork(unit(a))

  def run[A](es: ExecutorService)(p: Par[A]): A = {
    val ref = new AtomicReference[A]
    val latch = new CountDownLatch(1)

    p(es) { a => ref.set(a); latch.countDown }
    latch.await
    ref.get
  }

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
