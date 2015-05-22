package lecture.testing

import akka.actor.Actor

class TestToggle {

}

class Toggle extends Actor {
  def happy: Receive = {
    case "How are you?" =>
      sender ! "happy"
      context become sad
  }

  def sad: Receive = {
    case "How are you?" =>
      sender ! "sed"
      context become happy
  }

  def receive = happy
}
œ