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

  def or[F[_], G[_], H[_]](f: F ~> H, g: G ~> H): ({type cp[α]=Coproduct[F,G,α]})#cp ~> H = new NaturalTransformation[({type cp[α]=Coproduct[F,G,α]})#cp,H] {
    def apply[A](fa: Coproduct[F,G,A]): H[A] = fa.run match {
      case -\/(ff) ⇒ f(ff)
      case \/-(gg) ⇒ g(gg)
    }
  }

  type Language[A] = Coproduct[InteractOp, AuthOp, A]
  type LanguageCoyo[A] = Coyoneda[Language, A]
  type LanguageMonad[A] = Free[LanguageCoyo, A]
  def point[A](a: => A): FreeC[Language, A] = Monad[LanguageMonad].point(a)

  val interpreter: Language ~> Id = or(InteractConsole, AuthTest)

  def main(args: Array[String]) {
    def program(implicit I: Interact[Language], A: Auth[Language]) = {
      import I._
      import A._

      for {
        userId <- ask("Insert User ID: ")
        password <- ask("Password: ")
        user <- login(userId, password)
        hasPermission <- user.cata(
          none = point(false),
          some = hasPermission(_, "scalaz repository")
        )
        _ <- if (hasPermission) tell("Valid User") else tell("Invalid User")
      } yield hasPermission
    }

    program.mapSuspension(Coyoneda.liftTF(interpreter))
  }
}


