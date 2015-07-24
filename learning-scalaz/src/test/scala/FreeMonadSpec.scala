import org.scalatest.{Matchers, FunSuite}

/*
  ref:

  http://underscore.io/blog/posts/2015/04/14/free-monads-are-simple.html
  http://www.slideshare.net/kenbot/running-free-with-the-monads

 */

import scalaz._
import Scalaz._
import scalaz.Free._

class FreeMonadSpec extends FunSuite with Matchers {
  import KeyValueStore._

  test("liftF example") {
    // liftF :: F[A] => Free[F, A]

    val result = for {
      a <- liftF( Box(1) )
      b <- liftF( Box(2) )
      c <- liftF( Box(3) )
    } yield a + b + c

    println(result)
  }

  test("functor example") {
    val result = for {
      a <- liftF(() => 2 + 3)
      b <- liftF(() => a * 2)
      c <- liftF(() => a * b)
    } yield a + b + c
  }

  test("KVS example1") {
    val script: Free[KVS, Unit] = for {
      id <- get("swiss-bank-account-id")
      _ <- modify(id, (_ + 1000000))
      _ <- put("bermuda-airport", "getaway car")
      _ <- delete("tax-records")
    } yield ()

  }
}

object FreeMonadImpl {
  trait Free[F[_], A] {
    def flatMap[B](f: A => Free[F, B]): Free[F, B]
    def map[B](f: A => B): Free[F, B] = ???
  }

  case class Return[F[_], A](a: A) extends Free[F, A] {
    override def flatMap[B](f: (A) => Free[F, B]): Free[F, B] = f(a)
  }

//  case class Suspend[F[_], A](next: F[Free[F, A]]) extends Free[F, A] {
//    override def flatMap[B](f: (A) => Free[F, B]): Free[F, B] =
//      Suspend(next map {free => free flatMap f})
//  }
}

case class Box[A](a: A) {
  def map[B](f: A => B) = Box(f(a))
}

// ref: http://www.slideshare.net/kenbot/running-free-with-the-monads
object KeyValueStore {
  sealed trait KVS[Next]

  case class Put[Next](key: String, value: String, next: Next) extends KVS[Next]
  case class Delete[Next](key: String, next: Next) extends KVS[Next]
  case class Get[Next](key: String, onValue: String => Next) extends KVS[Next]

  new Functor[KVS] {
    override def map[A, B](kvs: KVS[A])(f: A => B): KVS[B] = kvs match {
      case Put(key, value, next) => Put(key, value, f(next))
      case Delete(key, next) => Delete(key, f(next))
      // to map over a function yielding the next value, compose `f` with it
      case Get(key, onResult) => Get(key, onResult andThen f)
    }
  }

  // initialize with Unit when we don't care about the value
  def put(key: String, value: String): Free[KVS, Unit] =
    liftF( Put(key, value, ()) )

  def delete(key: String): Free[KVS, Unit] =
    liftF( Delete(key, () ))

  // initialize with the id function, when we want to return a value
  // def identity[A](x: A): A = x
  def get(key: String): Free[KVS, String] =
    liftF( Get(key, identity) )

  def modify(key: String, f: String => String): Free[KVS, Unit] =
    for {
      v <- get(key)
      _ <- put(key, f(v))
    } yield ()

  // pure interpreters
  type KVStore = Map[String, String]

  def interpreterPure(kvs: Free[KVS, Unit], table: KVStore): KVStore =
  // F[Free[F, A]] \/ A
  // KVS[Free[KVS, Unit]] \/ Unit
    kvs.resume.fold({
      case Get(key, onResult) =>
        interpreterPure(onResult(table(key)), table)

      case Put(key, value, next) =>
        interpreterPure(next, table + (key -> value))

      case Delete(key, next) =>
        interpreterPure(next, table - key)

    }, _ => table /* When `resume` finally returns `Unit`, return the table */)

  type KVMStore = collection.mutable.Map[String, String]

  def interpreterImpure(kvs: Free[KVS, Unit], table: KVMStore): Unit =
    // def go(f: F[FreeF, A]] => Free[F, A]): A
    kvs.go {
      case Get(key, onResult) => onResult(table(key))
      case Put(key, value, next) =>
        table += (key -> value)
        next

      case Delete(key, next) =>
        table -= key
        next
    }
}










