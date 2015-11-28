package free.bank2

import scalaz._, Scalaz._, Free._, Inject._

object Application {
  def lift[F[_], G[_], A](fa: F[A])(implicit I: Inject[F, G]): FreeC[G, A] =
    Free.liftFC(I.inj(fa))

  def or[F[_], G[_], H[_]](f: F ~> H, g: G ~> H): ({type cp[α]=Coproduct[F,G,α]})#cp ~> H = new NaturalTransformation[({type cp[α]=Coproduct[F,G,α]})#cp,H] {
    def apply[A](fa: Coproduct[F,G,A]): H[A] = fa.run match {
      case -\/(ff) ⇒ f(ff)
      case \/-(gg) ⇒ g(gg)
    }
  }

  type Language0[A] = Coproduct[InteractOp, AuthOp, A]
  type Language[A] = Coproduct[LogOp, Language0, A]
  type LanguageCoyo[A] = Coyoneda[Language, A]
  type LanguageMonad[A] = Free[LanguageCoyo, A]
  def point[A](a: => A): FreeC[Language, A] = Monad[LanguageMonad].point(a)

  val interpreter0: Language0 ~> Id = or(InteractInterpreter, AuthInterpreter)
  val interpreter: Language ~> Id = or(LogInterpreter, interpreter0)

  def main(args: Array[String]) {
    def program(implicit I: Interact[Language], A: Auth[Language], L: Log[Language]) = {
      import I._
      import A._
      import L._

      for {
        userId <- ask("Insert User ID: ")
        password <- ask("Password: ")
        user <- login(userId, password)
        _ <- warn("")
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


