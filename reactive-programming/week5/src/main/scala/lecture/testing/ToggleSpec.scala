package lecture.testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import akka.util.Timeout
import org.scalatest.{Matchers, WordSpecLike}

import scala.util.Success
import scala.concurrent.duration._
import scala.concurrent.Await

class ToggleSpec extends TestKit(ActorSystem("TestSys"))
with ImplicitSender with WordSpecLike with Matchers {

  "Toggle Actor" should {
    "be happy at first" in {
      implicit val system = ActorSystem("TestSyste")

      val toggle = system.actorOf(Props[Toggle])

      val p = TestProbe()
      p.send(toggle, "How are you?")
      p.expectMsg("happy")

      p.send(toggle, "How are you?")
      p.expectMsg("sad")

      system.shutdown()
    }

    "then, actor will be sad" in {
      // using ImplicitSender, TestKit,
      val toggle = TestActorRef[Toggle]

      toggle ! "How are you?"
      expectMsg("happy")
      toggle ! "How are you?"
      expectMsg("sad")
    }

    "getting the underlying actor" in {
      val toggle = TestActorRef[Toggle]
      val a: Actor = toggle.underlyingActor
    }

    "be processed synchronously when instantiated by TestActorRef" in {
      val toggle = TestActorRef[Toggle]

      implicit val timeout = Timeout(1 seconds)

      // using ask pattern
      val future = toggle ? "How are you?"
      val Success(state: String) = future.value.get

      state should be ("happy")
    }
  }
}

class Toggle extends Actor {
  def happy: Receive = {
    case "How are you?" =>
      sender ! "happy"
      context become sad
  }

  def sad: Receive = {
    case "How are you?" =>
      sender ! "sad"
      context become happy
  }

  def receive = happy
}
