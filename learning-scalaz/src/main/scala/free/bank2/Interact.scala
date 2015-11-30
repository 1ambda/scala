package free.bank2

import scalaz.{Free, Inject, Id, ~>}, Free._, Id.Id

sealed trait InteractOp[A]
final case class Ask(prompt: String)   extends InteractOp[String]
final case class Tell(message: String) extends InteractOp[Unit]

class Interact[F[_]](implicit I: Inject[InteractOp, F]) {
  def ask(prompt: String): FreeC[F, String] =
    Common.lift(Ask(prompt))

  def tell(message: String): FreeC[F, Unit] =
    Common.lift(Tell(message))
}

object Interact {
  implicit def instance[F[_]](implicit I: Inject[InteractOp, F]): Interact[F] =
    new Interact
}

object InteractInterpreter extends (InteractOp ~> Id) {
  override def apply[A](fa: InteractOp[A]) = fa match {
    case Ask(prompt) =>
      println(prompt)
      scala.io.StdIn.readLine
    case Tell(message) =>
      println(message)
  }
}

