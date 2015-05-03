# Week 3

Try, Future, Async

### Try

example

```scala
package lecture

import org.junit.runner.RunWith
import org.scalatest.{FunSuite, Matchers, FunSpec}
import org.scalatest.junit.JUnitRunner

import scala.util.{Failure, Success, Try}

@RunWith(classOf[JUnitRunner])
class AdventureSpec extends FunSuite with Matchers {

  import Adventure._

  test("#Try") {
    val game = Adventure

    val result : Try[Treasure] = for {
      coins <- game.collectCoins()
      diamond <- game.buyTreasure(coins)
    } yield diamond

    result should be (Success(Diamond))
  }
}

object Adventure {

  case class GameOverException(message: String) extends RuntimeException
  case class Coin(value: Int)
  trait Treasure
  case object Diamond extends Treasure

  val treasureCost = 500

  def eatenByMonster = false

  def collectCoins(): Try[List[Coin]] = {
    if (eatenByMonster)
      Failure(GameOverException("Ooops"))
    else
      Success(List(Coin(100), Coin(400), Coin(99)))
  }

  def buyTreasure(coins: List[Coin]): Try[Treasure] = {
    if (coins.map(_.value).sum < treasureCost)
      Failure(GameOverException("Nice Try!"))
    else
      Success(Diamond)
  }
}
```

### Future

**blocking** example

```scala
package lecture

import org.junit.runner.RunWith
import org.scalatest.{FunSuite, FunSpec, Matchers}
import org.scalatest.junit.JUnitRunner

import scala.util.Random

@RunWith(classOf[JUnitRunner])
class SocketSpec extends FunSuite with Matchers {
  test("readFromMemory") {
    import Server._
    val socket = new Socket {}

    val packet = socket.readFromMemory()
    val result = socket.send(US, packet)

    packet.toList should be (List(1, 2, 3, 4))
    result.toList should be (List(5, 6, 7, 8))
  }
}

object Server {
  trait Server
  case object Europe extends Server
  case object US extends Server
}

trait Socket {
  import Server._

  def readFromMemory(): Array[Byte] = {
    sleepRandom
    List(1, 2, 3, 4).map(_.toByte).toArray
  }

  def send(to: Server, packet: Array[Byte]): Array[Byte] = {
    sleepRandom
    List(5, 6, 7, 8).map(_.toByte).toArray
  }

  def sleepRandom = Thread.sleep(Random.nextInt(500));
}

```

**non-blocking** example

```scala
@RunWith(classOf[JUnitRunner])
class AsyncSocketSpec extends FunSuite with ShouldMatchers with AsyncAssertions with ScalaFutures {
  import Server._

  val limit = timeout(Span(2000, Millis))

  // for whenReady
  implicit val defaultPatience = PatienceConfig(timeout = Span(2, Seconds))

  test("non-blocking1") {

    val socket = new AsyncSocket {}

    val response = for {
      packet <- socket.readFromMemoryAsync()
      result <- socket.sendAsync(Europe, packet)
    } yield result

    whenReady(response) {
      res => res.toList should be (List(5, 6, 7, 8))
    }
  }

  test("non-blocking2") {
    val w = new Waiter

    val socket = new AsyncSocket {}
    val packet: Future[Array[Byte]] = socket.readFromMemoryAsync()

    packet.onComplete {
      case Success(p) =>
        val result: Future[Array[Byte]] = socket.sendAsync(Europe, p)

        result.onComplete {
          case Success(b) =>
            b.toList should be (List(5, 6, 7, 8))
            w.dismiss()
          case Failure(t) =>
        }

      case Failure(t) =>
    }

    w.await(limit)
  }
}

trait AsyncSocket {
  import Server._
  import Sleep._

  def readFromMemoryAsync(): Future[Array[Byte]] = Future {
    sleepRandom
    List(1, 2, 3, 4).map(_.toByte).toArray
  }

  def sendAsync(to: Server, packet: Array[Byte]): Future[Array[Byte]] = Future {
    sleepRandom
    val received = packet.toList
    // do something with packet
    received.map(x => (x + 4).toByte).toArray
  }
}
```

### recoverWith, fallbackTo

HTTP modeling

```scala
object Http extends Socket {

  class HttpMethod
  object HttpMethod {
    case object POST extends HttpMethod
    case object GET extends HttpMethod
    case object PUT extends HttpMethod
    case object DELETE extends HttpMethod
  }

  class HttpStatue
  object HttpStatus {
    case object isOK extends HttpStatue
  }

  case class Request(body: Array[Byte], method: HttpMethod)
  case class Response(body: Array[Byte], status: HttpStatue)

  def apply(url: URL, req: Request): Future[Response] = Future {
    sleepRandom
    Response(req.body.toList.map(x => (x + 4).toByte).toArray, HttpStatus.isOK)
  }
}
```

We are going to use `recover`, `recoverWith`, `fallbackTo` . They look like

```scala
def recover(f: PartuialFunction[Throwable, T]): Future[T]
def recoverWith(f: PartialFunction[Throwable, Future[T]]): Future[T]

def fallbackTo(that: => Future[T]): Future[T] = {
  this recoverWith {
    case _ => that recoverWith { case _ => this }
  }
}
```

using these method, we can implement `ResilientSocket` which never fail.


```scala
trait ResilientSocket extends Socket with mailServer {
  import Http._

  def sendToFallback(packet: Array[Byte]): Future[Array[Byte]] = {
    send(Europe, packet) fallbackTo {
      send(USA, packet)
    } recover {
      case t: FailToSendException =>
        // default value when two sending requests failed
        packet.toList.map(x => (x + 4).toByte).toArray
    }
  }

  def sendToRecover(packet: Array[Byte]): Future[Array[Byte]] = {
    send(Europe, packet) recoverWith {
      case FailToSendException(region) =>
        send(USA, packet) recover {
          case t: FailToSendException =>
            // default value when two sending requests failed
            packet.toList.map(x => (x + 4).toByte).toArray
        }
    }
  }

  def sendToEurope(packet: Array[Byte]): Future[Array[Byte]] =
  send(Europe, packet)

  def send(url: URL, packet: Array[Byte]): Future[Array[Byte]] =
    if (Random.nextBoolean() == true)
      url match {
        case Europe => Future.failed(FailToSendException(Europe))
        case USA => Future.failed(FailToSendException(USA))
      }
    else {
      Http(url, Request(packet, HttpMethod.POST))
        .filter(_.status == HttpStatus.isOK)
        .map(_.body)
    }
}
```

### Await

`Awaitable` is trait providing capability to program imperatively (blocking)

```scala
trait Awaitable[T] extends AnyRef {
  abstract def ready(atMost: Duration): Unit
  abstract def result(atMost: Duration): T
}

trait Future[T] extends Awaitable[T]
```

### Retry 

using recursion

```scala
def retry[T](times: Int)(block: => Future[T]): Future[T] = {
  if (times == 0) Future.failed(new RuntimeException("Time 0 Exception"))
  else block fallbackTo {
    retry(times - 1) { block }
  }
}

// test
test("retry using recursion") {
  val w = new Waiter

  val confirm = for {
    packet <- socket.asyncReadFromMemory()
    confirm <- socket.retry(10) { socket.send(Europe, packet) }
  } yield confirm

  confirm.onComplete {
    case Success(response) =>
      response should be (expected)
      w.dismiss()
    case Failure(t: FailToSendException) =>
      t.url should be (Europe)
      w.dismiss()
  }

  w.await(limit)
}
```

`retry` can be implemented using `foldLeft` or `foldRight` 

```scala
def foldLeftRetry[T](times: Int)(block: => Future[T]): Future[T] = {
  val failed: Future[T] = Future.failed(new RuntimeException("failed Future"))
  val blocks = (1 to times).map(_ => () => block)
  blocks.foldLeft(failed){
    (err, b) => err recoverWith { case _ => b() }
  }
}

def foldRightRetry[T](times: Int)(block: => Future[T]): Future[T] = {
  val failed: Future[T] = Future.failed(new RuntimeException("failed Future"))
  val blocks = (1 to times).map(_ => () => block)
  blocks.foldRight(() => failed) {
    (b, err) => () => { b() fallbackTo { err() }}
  }()
}
```

`await` provides the capability of modeling imperative programming. But before using `await`,
  read the following uses when await are illegal and are reported as errors.
  
- `await` requires a directly-enclosing async. This means await must not be used inside a closure
 nested within an async block, or insde a nested object, trait, or class
 
- `await` must not be used inside an expression passed as an argument to a by-name parameter

- `await` must not be used inside a Boolean short-circuit argument.

- return expressions are illegal inside an async block

- `await` **should not be used under a try/catch**
 

```scala
// illegal usage of await 1: yield await
 
async { 
 for { x <- someAction() } yield await (x)
} 

// illegal usage of await 2: inside an expression 

async {
  someValue.someFunction(x => await(x))
}
```


### Retry: Using `await`

```
// package.scala
package object lecture {
  // implicit classes 
  // ref: http://www.blog.project13.pl/index.php/coding/1769/scala-2-10-and-why-you-will-love-implicit-value-classes/
  implicit class TryCompanionOps(val t: Try.type) extends AnyVal {
    def convertTriedFuture[T](f: => Future[T]): Future[Try[T]] = f.map(value => Try(value))
  }
}

// ResilientSocket.scala
def awaitRetry[T](times: Int)(block: => Future[T]): Future[T] = async {
  var i = 0
  var result: Try[T] = Failure(new RuntimeException("failure"))

  while (result.isFailure && i < times) {
    result = await { Try.convertTriedFuture(block) }
    i += 1
  }

  result.get
}
```

We could reimplement `filter`, `flatMap` with `await`

```scala
def filter(pred: T => Boolean): Future[T] = async {
  val x = await { this }
  if (!pred(x)) throw new NoSuchElementException()
  else x
}

def flatMap[S](f: T => Future[S]): Future[S] = async {
  val x = await { this }
  await { f(x) }
}
```


### Promise

```scala
// no async version filter
def filter(pred: T => Boolean): Future[T] = {
  val p = Promise[T]()
  
  this onComplete {
    case Failure(t) => p.failure(t)
    case Success(x) => 
      if (!pred(X) => p.failure(new NoSuchElementException)
      else p.success(x)
  }
  
  p.future
}
```

Promise can be used racing two futures. (e.g. caching)

```scala
def randomSleep = Thread.sleep(Random.nextInt(500))
def getAccountFromRemote: Future[String] = Future {
  randomSleep
  "A9-20401-7D-REMOTE"
}

def getAccountFromLocal: Future[String] = Future {
  randomSleep
  "A9-20401-7D-LOCAL"
}

def getAccount() = {
  val p = Promise[String]()

  val local = getAccountFromLocal
  val remote = getAccountFromRemote

  remote onComplete { p.tryComplete(_) }
  local  onComplete { p.tryComplete(_) }

  p.future
}

test("promise can be used racing 2 futures") {

  val w = new Waiter
  val limit = timeout(Span(2, Seconds))

  getAccount onComplete {
    case Success(acc) =>
      acc should (
        equal ("A9-20401-7D-REMOTE") or
        equal ("A9-20401-7D-LOCAL")
      )
      w.dismiss()
    case _ => w.dismiss()
  }

  w.await(limit)
}
```

### Reimplementing `zip`

```scala
// using `Promise`

def zip[S, R](p: Future[S], f: (T, S) => R): Future[R] = {
  val p = Promise[R]
  
  this onComplete {
    case Failure(e1) => p.failure(e1)
    case Success(x) => that onComplete {
      case Failure(e2) => p.failure(e2)
      case Success(y) => p.success(f(x, y))
    }
  }
}

// using `awiat`

def zip[S, R](p: Future[S], f: (T, S) => R): Future[R] = async {
  f(await { this }, await { that })
}
```

<br/>

### Implementing `sequence` with `await`

```scala
def randomSleep = Thread.sleep(Random.nextInt(200))
def randomFutures: List[Future[String]] = (1 to 10).toList.map(x => Future {
  randomSleep
  x.toString
})

def awaitSequence[T](fs: List[Future[T]]): Future[List[T]] = async {
  var _fs = fs
  val r = ListBuffer[T]()

  while (_fs != Nil) {
    r.append(await { _fs.head })
    _fs = _fs.tail
  }
  r.toList
}

def recursiveSequence[T](fts: List[Future[T]]): Future[List[T]] = {
    fts match {
      case Nil => Future(Nil)
      case (ft::fts) => ft.flatMap(t => recursiveSequence(fts)
                            .flatMap(ts => Future(t::ts)))
    }

    fts match {
      case Nil => Future(Nil)
      case (ft::fts) => 
        for {
          t <- ft
          ts <- recursiveSequence(fts)
        } yield t::ts
    }
}
```
