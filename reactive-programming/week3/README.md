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

