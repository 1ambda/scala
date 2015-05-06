package nodescala


import scala.language.postfixOps
import scala.util.{Random, Try, Success, Failure}
import scala.collection._
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.async.Async.{async, await}
import org.scalatest._
import NodeScala._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class NodeScalaSuite extends FunSuite {
  def delayed[T](x: T) = Future { blocking { randomSleep; x } }
  def instantly[T](x: T) = Future { x }
  def failed = Future.failed(new RuntimeException)
  def randomSleep = Thread.sleep(Random.nextInt(400))

  // ref: https://class.coursera.org/reactive-002/forum/thread?thread_id=436
  test("CancellationTokenSource should allow stopping the computation") {
    val p = Promise[String]()

    val cts = Future.run() { ct =>
      async {
        while (ct.nonCancelled) {
          val a = 3
          a + 4
        }
        p.success("cancelled")
      }
    }
    cts.unsubscribe()
    assert(Await.result(p.future, 1 second) == "cancelled")
  }

  // ref: https://class.coursera.org/reactive-002/forum/thread?thread_id=436
  test("CancellationTokenSource should allow completion if not cancelled") {
    val p = Promise[String]()

    Future.run() { ct =>
      async {
        while(ct.nonCancelled) {
          p.success("doing")
        }

        p.success("cancelled")
      }
    }

    assert(Await.result(p.future, 1 second) != "cancelled")
  }

  // ref: https://class.coursera.org/reactive-002/forum/thread?thread_id=511
  test("continueWith should wait for the first future to complete") {

    val delay = Future.delay(1 second)
    val always = (f: Future[Unit]) => 42

    try {
      Await.result(delay.continueWith(always), 500 millis)
      assert(false)
    } catch {
      case t: TimeoutException => assert(true)
    }
  }

  // ref: https://class.coursera.org/reactive-002/forum/thread?thread_id=640
  test("now: Success") {
    val p = Promise[Unit]()
    p.success(())
    p.future.now
    assert(true)
  }

  test("now: Failure") {
    intercept[NoSuchElementException] {
      delayed(3).now
    }
  }

  test("A Future should always be completed") {
    val always = Future.always(517)
    assert(Await.result(always, 0 nanos) == 517)
  }

  test("A Future should never be completed") {
    val never = Future.never[Int]

    try {
      Await.result(never, 1 second)
      assert(false)
    } catch {
      case t: TimeoutException => // ok!
    }
  }

  test("Future.all Success case") {
    val fs = (3 to 5).toList.map(delayed(_))
    assert(Await.result(Future.all(fs), 2 seconds) == List(3, 4, 5))
  }

  test("Future.all Failure case") {
    val fs1 = failed :: (3 to 5).toList.map(delayed(_))
    val fs2 = delayed(3) :: delayed(4) :: failed :: Nil

  }

  test("Promise isCompleted test") {
    val p = Promise()
    p.failure(new RuntimeException)

    assert(p.isCompleted)
  }

  test("any") {
    val five = Future.any(List(delayed(3), delayed(4), instantly(5)))
    assert(5 == Await.result(five, 2 seconds))
  }

  test("ensuring") {
      val a: Future[String] = Future {
        randomSleep
        "Success"
      }

      val b: Future[String] = Future {
        randomSleep
        "Success2"
      }

    val ensured = a ensure b
    assert(Await.result(ensured, 3 seconds) == "Success")
  }

  
  class DummyExchange(val request: Request) extends Exchange {
    @volatile var response = ""
    val loaded = Promise[String]()
    def write(s: String) {
      response += s
    }
    def close() {
      loaded.success(response)
    }
  }

  class DummyListener(val port: Int, val relativePath: String) extends NodeScala.Listener {
    self =>

    @volatile private var started = false
    var handler: Exchange => Unit = null

    def createContext(h: Exchange => Unit) = this.synchronized {
      assert(started, "is server started?")
      handler = h
    }

    def removeContext() = this.synchronized {
      assert(started, "is server started?")
      handler = null
    }

    def start() = self.synchronized {
      started = true
      new Subscription {
        def unsubscribe() = self.synchronized {
          started = false
        }
      }
    }

    def emit(req: Request) = {
      val exchange = new DummyExchange(req)
      if (handler != null) handler(exchange)
      exchange
    }
  }

  class DummyServer(val port: Int) extends NodeScala {
    self =>
    val listeners = mutable.Map[String, DummyListener]()

    def createListener(relativePath: String) = {
      val l = new DummyListener(port, relativePath)
      listeners(relativePath) = l
      l
    }

    def emit(relativePath: String, req: Request) = this.synchronized {
      val l = listeners(relativePath)
      l.emit(req)
    }
  }
  test("Server should serve requests") {
    val dummy = new DummyServer(8191)
    val dummySubscription = dummy.start("/testDir") {
      request => for (kv <- request.iterator) yield (kv + "\n").toString
    }

    // wait until server is really installed
    Thread.sleep(500)

    def test(req: Request) {
      val webpage = dummy.emit("/testDir", req)
      val content = Await.result(webpage.loaded.future, 1 second)
      val expected = (for (kv <- req.iterator) yield (kv + "\n").toString).mkString
      assert(content == expected, s"'$content' vs. '$expected'")
    }

    test(immutable.Map("StrangeRequest" -> List("Does it work?")))
    test(immutable.Map("StrangeRequest" -> List("It works!")))
    test(immutable.Map("WorksForThree" -> List("Always works. Trust me.")))

    dummySubscription.unsubscribe()
  }

}




