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

  def _choice[A](cond: Par[Boolean])(t: Par[A], f: Par[A]): Par[A] =
    _choiceN(map(cond)(x => if (x) 0 else 1))(List(t, f))

  def _choiceN[A](n: Par[Int])(choices: List[Par[A]]): Par[A] =
    es => new Future[A] {
      override private[chapter7] def apply(callback: (A) => Unit): Unit =
        n(es) { index =>
          eval(es) { choices(index)(es)(callback) }
        }
    }

  def flatMap[A, B](p: Par[A])(f: A => Par[B]): Par[B] =
    es => new Future[B] {
      override private[chapter7] def apply(callback: (B) => Unit): Unit =
        p(es) { a =>
          eval(es) { f(a)(es)(callback) }
        }
    }

  def choice[A](cond: Par[Boolean])(t: Par[A], f: Par[A]): Par[A] =
    flatMap(cond)(x => if (x) t else f)

  def choiceN[A](n: Par[Int])(choices: List[Par[A]]): Par[A] =
    flatMap(n)(index => choices(index))

  // using flatMap
  // def join[A](p: Par[Par[A]]): Par[A] =
  //   flatMap(p)(x => x)

  def join[A](p: Par[Par[A]]): Par[A] =
    es => new Future[A] {
      override private[chapter7] def apply(callback: (A) => Unit): Unit =
        p(es) { p2 =>
          eval(es) { p2(es)(callback) }
        }
    }

  // support infix notation
  implicit def toParOps[A](p : Par[A]): ParOps[A] = new ParOps(p)

  class ParOps[A](p: Par[A]) {
    def map[B](f: A => B): Par[B] = Par.map(p)(f)
    def map2[B, C](b: Par[B])(f: (A, B) => C): Par[C] = Par.map2(p, b)(f)
    def flatMap[B](f: A => Par[B]): Par[B] = Par.flatMap(p)(f)
    def zip[B](b: Par[B]): Par[(A, B)] = p.map2(b)((_, _))
  }
}
