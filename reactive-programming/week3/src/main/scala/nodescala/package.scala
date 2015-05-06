import scala.collection.mutable.ListBuffer
import scala.language.postfixOps
import scala.io.StdIn
import scala.util._
import scala.util.control.NonFatal
import scala.concurrent._
import scala.concurrent.duration._
import ExecutionContext.Implicits.global
import scala.async.Async.{async, await}

/** Contains basic data types, data structures and `Future` extensions.
 */
package object nodescala {
  // extends the Future companion object
  implicit class FutureCompanionOps(val f: Future.type) extends AnyVal {

    // Returns a future that is always completed with `value`.
    def always[T](value: T): Future[T] = {
      val p = Promise[T]()
      p.complete(Success(value))
      p.future
    }

    // Returns a future that is never completed.
    def never[T]: Future[T] = {
      val p = Promise[T]()
      p.failure(new TimeoutException)
      p.future
    }

    /** Given a list of futures `fs`, returns the future holding the list of values of all the futures from `fs`.
     *  The returned future is completed only once all of the futures in `fs` have been completed.
     *  The values in the list are in the same order as corresponding futures `fs`.
     *  If any of the futures `fs` fails, the resulting future also fails.
     */
    def all[T](fs: List[Future[T]]): Future[List[T]] =  fs match {
        case Nil => Future(Nil)
        case ft::fts => for {
          x <- ft
          xs <- all(fts)
        } yield (x::xs)
    }
    //returns the future holding the value of the future from `fs` that completed first.
    def any[T](fs: List[Future[T]]): Future[T] = {
      val p = Promise[T]()
      val completeFirst: Try[T] => Unit = p tryComplete _
      fs foreach { _ onComplete completeFirst}
      p.future
    }

    /** Returns a future with a unit value that is completed after time `t`.
     */
    def delay(t: Duration): Future[Unit] = Future {
      blocking {
        Thread.sleep(t.toMillis)
      }
    }

    /** Completes this future with user input.
     */
    def userInput(message: String): Future[String] = Future {
      blocking {
        StdIn.readLine(message)
      }
    }

    /** Creates a cancellable context for an execution and runs it.
     */
    def run()(f: CancellationToken => Future[Unit]): Subscription = {
      val subscription: CancellationTokenSource = CancellationTokenSource()
      val token: CancellationToken = subscription.cancellationToken
      f(token)
      subscription
    }

  }

  /** Adds extension methods to future objects.
   */
  implicit class FutureOps[T](val f: Future[T]) extends AnyVal {

    def ensure[S](that: Future[S]): Future[T] = {
      val p = Promise[T]()

      f onComplete {
        case tryValue =>
          that onComplete {
            case Success(_) => p.complete(tryValue)
            case Failure(t) => p.failure(t)
          }
      }

      p.future
    }

    /** Returns the result of this future if it is completed now.
     *  Otherwise, throws a `NoSuchElementException`.
     *
     *  Note: This method does not wait for the result.
     *  It is thus non-blocking.
     *  However, it is also non-deterministic -- it may throw or return a value
     *  depending on the current state of the `Future`.
     */
    def now: T = try {
      Await.result(f, 0 seconds)
    } catch {
      case t: Throwable => throw new NoSuchElementException()
    }

    /** Continues the computation of this future by taking the current future
     *  and mapping it into another future.
     *
     *  The function `cont` is called only after the current future completes.
     *  The resulting future contains a value returned by `cont`.
     */
    def continueWith[S](cont: Future[T] => S): Future[S] = f map (x => cont(Future(x)))
      //for { x <- f } yield cont(Future(x))


    /** Continues the computation of this future by taking the result
     *  of the current future and mapping it into another future.
     *
     *  The function `cont` is called only after the current future completes.
     *  The resulting future contains a value returned by `cont`.
     */
    def continue[S](cont: Try[T] => S): Future[S] = f map (x => cont(Success(x)))
  }

  /** Subscription objects are used to be able to unsubscribe
   *  from some event source.
   */
  trait Subscription {
    def unsubscribe(): Unit
  }

  object Subscription {
    /** Given two subscriptions `s1` and `s2` returns a new composite subscription
     *  such that when the new composite subscription cancels both `s1` and `s2`
     *  when `unsubscribe` is called.
     */
    def apply(s1: Subscription, s2: Subscription) = new Subscription {
      def unsubscribe() {
        s1.unsubscribe()
        s2.unsubscribe()
      }
    }
  }

  /** Used to check if cancellation was requested.
   */
  trait CancellationToken {
    def isCancelled: Boolean
    def nonCancelled = !isCancelled
  }

  /** The `CancellationTokenSource` is a special kind of `Subscription` that
   *  returns a `cancellationToken` which is cancelled by calling `unsubscribe`.
   *
   *  After calling `unsubscribe` once, the associated `cancellationToken` will
   *  forever remain cancelled -- its `isCancelled` will return `false.
   */
  trait CancellationTokenSource extends Subscription {
    def cancellationToken: CancellationToken
  }

  /** Creates cancellation token sources.
   */
  object CancellationTokenSource {
    /** Creates a new `CancellationTokenSource`.
     */
    def apply() = new CancellationTokenSource {
      val p = Promise[Unit]()
      val cancellationToken = new CancellationToken {
        def isCancelled = p.future.value != None
      }
      def unsubscribe() {
        p.trySuccess(())
      }
    }
  }
}

