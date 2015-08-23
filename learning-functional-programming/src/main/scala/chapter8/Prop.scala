package chapter8

import java.util.concurrent.Executors

import Prop._
import util.{RNG, Par}
import Par.Par

case class Prop(run: (MaxSize, TestCaseCount, RNG) => Result) {
  def check: Either[(String, Int), Int] = ???

  def &&(p: Prop) = Prop {
    (max,n,rng) => run(max,n,rng) match {
      case Passed | Proved => p.run(max, n, rng)
      case x => x
    }
  }

  def ||(p: Prop) = Prop {
    (max,n,rng) => run(max,n,rng) match {
      // In case of failure, run the other prop.
      case Falsified(msg, _) => p.tag(msg).run(max,n,rng)
      case x => x
    }
  }

  /* This is rather simplistic - in the event of failure, we simply prepend
   * the given message on a newline in front of the existing message.
   */
  def tag(msg: String) = Prop {
    (max,n,rng) => run(max,n,rng) match {
      case Falsified(e, c) => Falsified(msg + "\n" + e, c)
      case x => x
    }
  }
}

sealed trait Result {
  def isFalsified: Boolean
}

case object Passed extends Result {
  override def isFalsified: Boolean = false
}

case class Falsified(failure: FailedCase, successes: SuccessCount) extends Result {
  def isFalsified = true
}

case object Proved extends Result {
  def isFalsified = false
}

object Prop {
  type FailedCase = String
  type SuccessCount = Int
  type TestCaseCount = Int
  type MaxSize = Int

  def forAll[A](as: Gen[A])(f: A => Boolean): Prop = Prop {
    (n,rng) => randomStream(as)(rng).zip(util.Stream.from(0)).take(n).map {
      case (a, i) => try {
        if (f(a)) Passed else Falsified(a.toString, i)
      } catch { case e: Exception => Falsified(buildMsg(a, e), i) }
    }.find(_.isFalsified).getOrElse(Passed)
  }

  def randomStream[A](g: Gen[A])(rng: RNG): util.Stream[A] =
    util.Stream.unfold(rng)(rng => Some(g.sample.run(rng)))

  def buildMsg[A](s: A, e: Exception): String =
    s"test case: $s\n" +
      s"generated an exception: ${e.getMessage}\n" +
      s"stack trace:\n ${e.getStackTrace.mkString("\n")}"

  def forAll[A](g: SGen[A])(f: A => Boolean): Prop =
    forAll(g(_))(f)

  def forAll[A](g: Int => Gen[A])(f: A => Boolean): Prop = Prop {
    (max /* size */, n /* case count */, rng) =>

      val casesPerSize = (n + (max - 1)) / max

      val props: util.Stream[Prop] =
        util.Stream
          .from(0)
          .take((n min max) + 1)
          .map(index => forAll(g(index))(f)) /* call apply */

      val prop: Prop =
        props.map(p => Prop { (max, _, rng) =>
          p.run(max, casesPerSize, rng)
        }).toList.reduce(_ && _)

      prop.run(max, n, rng)
  }

  def apply(f: (TestCaseCount, RNG) => Result): Prop =
    Prop { (_,n,rng) => f(n,rng) }

  def run(p: Prop,
          maxSize: MaxSize = 100,
          count: TestCaseCount = 100,
          rng: RNG = RNG.Simple(System.currentTimeMillis())): Unit = {

    p.run(maxSize, count, rng) match {
      case Falsified(message, successCount) =>
        println(s"! Falsified after $successCount passed tests:\n $message")
      case Passed =>
        println(s"+ OK, passed $count tests")
      case Proved =>
        println(s"+ OK, proved Property")
    }
  }

  def check(p: Boolean): Prop = Prop { (_, _, _) =>
    if (p) Proved else Falsified("()", 0)
  }

  // for Par testing
  import Gen._
  val randomExecutors = weighted(
    choose(1,4).map(Executors.newFixedThreadPool) -> .75,
    unit(Executors.newCachedThreadPool) -> .25) // `a -> b` is syntax sugar for `(a,b)`

  def forAllPar2[A](g: Gen[A])(f: A => Par[Boolean]): Prop =
    forAll(randomExecutors.map2(g)((_,_))) { case (es,a) => f(a)(es).get }

  def forAllPar3[A](g: Gen[A])(f: A => Par[Boolean]): Prop =
    forAll(randomExecutors ** g) { case (es, a) => f(a)(es).get }

  def forAllPar[A](g: Gen[A])(f: A => Par[Boolean]): Prop =
    forAll(randomExecutors ** g) { case es ** a => f(a)(es).get } /* unapply */

  def checkPar(p: Par[Boolean]) = forAllPar(Gen.unit())(_ => p)
}

object ** {
  def unapply[A, B](p: (A, B)) = Some(p)
}
