package composition

import org.scalatest._

// ref: http://danielwestheide.com/
class CompositionTest extends FlatSpec with Matchers {

  case class Email(
    subject: String,
    text: String,
    sender: String,
    recipient: String
  )

  type EmailFilter = Email => Boolean

  def checkEmail(mails: Seq[Email], f: EmailFilter) = mails.filter(f)
  def notSentByAnyOf: Set[String] => EmailFilter =
    senders => email => !senders.contains(email.sender)

  val emails = List(Email(
      subject = "It's me again, your stalker friend!",
      text = "Hello my friend! How are you?",
      sender = "johndoe@example.com",
      recipient = "me@example.com"))

  "email which sent by john" should "be filtered" in  {
    val checked = checkEmail(emails, notSentByAnyOf(Set("johndoe@example.com")))
    checked.size should be (0)
  }
}
