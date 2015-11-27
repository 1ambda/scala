package free.bank2

import scalaz._, Scalaz._, Free._

sealed trait InteractOp[A]
final case class Ask(prompt: String)   extends InteractOp[String]
final case class Tell(message: String) extends InteractOp[Unit]

class Interact[F[_]](implicit I: Inject[InteractOp, F]) {

  /**
   * Inject using reflexiveInjectInstance which turns F[A] into F[A] (identity)
   *
   * implicit def reflexiveInjectInstance[F[_]] =
   * new Inject[F, F] {
   * def inj[A](fa: F[A]) = fa
   * def prj[A](ga: F[A]) = some(ga)
   * }
   */
  def ask(prompt: String): FreeC[F, String] = App.lift(Ask(prompt))
  def tell(message: String): FreeC[F, Unit] = App.lift(Tell(message))
}

object Interact {
  implicit def instance[F[_]](implicit I: Inject[Interact, F]): Interact[F]
    = new Interact[F]
}





