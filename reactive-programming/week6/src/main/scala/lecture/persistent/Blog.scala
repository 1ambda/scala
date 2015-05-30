package lecture.persistent

import akka.persistence.PersistentActor

case class NewPost(text: String, id: Long)
case class BlogPosted(id: Long)
case class BlogNotPosted(id: Long, reason: String)

sealed trait Event
case class PostCreated(text: String) extends Event
case object QuotaReached             extends Event

case class State(posts: Vector[String], disabled: Boolean) {
  def updated(e: Event): State = e match {
    case PostCreated(text) => copy(posts = posts :+ text)
    case QuotaReached      => copy(disabled = true)
  }
}

class UserProcessor extends PersistentActor {

  var state = State(Vector.empty, false)

  def receiveCommand = {
    case NewPost(text, id) =>
      if (state.disabled) sender() ! BlogNotPosted(id, "quota reached")
      else {
        persist(PostCreated(text)) { e =>
          updateState(e)
          sender() ! BlogPosted(id)
        }

        persist(QuotaReached)(updateState)
      }
  }

  def updateState(e: Event) { state = state.updated(e) }
  def receiveRecover = { case e: Event => updateState(e) }

  override def persistenceId: String = ???
}

