package free.bank2

import scalaz._, Scalaz._, Free._

object App {

  /**
   * sealed abstract class Inject[F[_], G[_]] {
   *  def inj[A](fa: F[A]): G[A]
   *  def prj[A](ga: G[A]): Option[F[A]]
   * }
   */
  def lift[F[_], G[_], A](fa: F[A])(implicit I: Inject[F, G]): FreeC[G, A] =
    Free.liftFC(I.inj(fa))



  def program[F[_]](implicit I: Interact[F]): Unit = {
    import I._

    for {
      uid <- ask("Insert User ID: ")
      pwd <- ask("Password: ")
    } yield ()
  }

  type withInteract[A] = Coyoneda[Interact, A]
  type Application[A] = Free[withInteract, A]



}
