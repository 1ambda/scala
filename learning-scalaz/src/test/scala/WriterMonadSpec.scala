import org.scalatest.{Matchers, FunSuite}

import scala.annotation.tailrec

// ref: http://tonymorris.github.io/blog/posts/the-writer-monad-using-scala-example/
// ref: http://eed3si9n.com/learning-scalaz/Writer.html

/*

  type Writer[+W, +A] = WriterT[Id, W, A]

  sealed trait WriterT[F[+_], +W, +A] { self =>
    val run: F[(W, A)]

    def written(implicit F: Functor[F]): F[W] =
      F.map(run)(_._1)
    def value(implicit F: Functor[F]): F[A] =
      F.map(run)(_._2)

    ...
    ...
 }

 */

import scalaz._
import Scalaz._

class WriterMonadSpec extends FunSuite with Matchers {
  test("WriterV Basics") {
    /* ref: https://github.com/scalaz/scalaz/blob/series/7.1.x/core/src/main/scala/scalaz/syntax/WriterOps.scala

      final class WriterOps[A](self: A) {
        def set[W](w: W): Writer[W, A] = WriterT.writer(w -> self)

        def tell: Writer[A, Unit] = WriterT.tell(self)
      }

      trait ToWriterOps {
        implicit def ToWriterOps[A](a: A) = new WriterOps(a)
      }

     */

    3.set("Three") shouldBe Writer[String, Int]("Three", 3)
    3.tell shouldBe Writer[Int, Unit](3, ())
    MonadTell[Writer, String].point(3).run shouldBe ("", 3)
  }

  test("Writer Basics") {

    def logNumber(x: Int): Writer[List[String], Int] =
      x.set(List("Got number: " + x.shows))

    def multiWithLog: Writer[List[String], Int] = for {
      a <- logNumber(3)
      b <- logNumber(5)
    } yield a * b // will be 15

    multiWithLog.run shouldBe (List("Got number: 3", "Got number: 5"), 15)
  }

  test("GCD example") {
    def gcd(a: Int, b: Int): Writer[List[String], Int] =
      if (b == 0) for {
        _ <- List("Finished with " + a.shows).tell
      } yield a
      else List(a.shows + "mod" + b.shows + " = " (a % b).shows).tell >>= { _ =>
        gcd(b, a % b)
      }

    gcd(8, 3).run._2 shouldBe 1
  }

  test("fast gcd using Vector instead of Vector") {
    def gcd(a: Int, b: Int): Writer[Vector[String], Int] =
      if (b == 0) for {
        _ <- Vector("Finished with " + a.shows).tell
      } yield a
      else Vector(a.shows + "mod" + b.shows + " = " (a % b).shows).tell >>= { _ =>
        gcd(b, a % b)
      }

    gcd(8, 3).run._2 shouldBe 1
  }

  test("Vector, List Performance comparison") {
    def vectorCountdown(x: Int): Writer[Vector[String], Unit] = {
      @tailrec
      def recur(x: Int)(w: Writer[Vector[String], Unit]): Writer[Vector[String], Unit] =
        x match {
          case 0 => w flatMap { _ => Vector("0").tell }
          case x => recur(x - 1)(w flatMap { _ => Vector(x.shows).tell })
        }

      val start = System.currentTimeMillis
      val r = recur(x)(Vector[String]().tell)
      val end = System.currentTimeMillis
      r flatMap { _ => Vector((end - start).shows).tell }
    }

    def listCountdown(x: Int): Writer[List[String], Unit] = {
      @tailrec
      def recur(x: Int)(w: Writer[List[String], Unit]): Writer[List[String], Unit] =
        x match {
          case 0 => w flatMap { _ => List("0").tell }
          case x => recur(x - 1)(w flatMap { _ => List(x.shows).tell })
        }

      val start = System.currentTimeMillis
      val r = recur(x)(List[String]().tell)
      val end = System.currentTimeMillis
      r flatMap { _ => List((end - start).shows).tell }
    }


    /* milliseconds */
    val vectorPerf = vectorCountdown(10000).run._1.last.toInt
    val listPerf = listCountdown(10000).run._1.last.toInt

    vectorPerf should be < listPerf
  }
}


