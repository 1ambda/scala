package free.bank1

import scalaz._, Scalaz._, Free._, Inject._

object Interact {

  trait InteractOp[A]
  final case class Ask(prompt: String) extends InteractOp[String]
  final case class Tell(msg: String)   extends InteractOp[Unit]

  type CoyonedaInteract[A] = Coyoneda[InteractOp, A]
  type Interact[A] = Free[CoyonedaInteract, A]

  def ask(prompt: String) = liftFC(Ask(prompt))
  def tell(msg: String) = liftFC(Tell(msg))

  object Console extends (InteractOp ~> Id) {
    override def apply[A](i: InteractOp[A]): Id[A] = i match {
      case Ask(prompt) =>
        println(prompt)
        readLine()

      case Tell(msg) =>
        println(msg)
    }
  }
}

