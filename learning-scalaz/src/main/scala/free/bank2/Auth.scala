package free.bank2

import Auth._

import scalaz.{Free, Inject, Id, ~>}, Free._, Id.Id

case class User(userId: UserId, permissions: Set[Permission])

sealed trait AuthOp[A]
final case class Login(userId: UserId, password: Password) extends AuthOp[Option[User]]
final case class HasPermission(user: User, permission: Permission) extends AuthOp[Boolean]

object Auth {
  type UserId = String
  type Password = String
  type Permission = String

  implicit def instance[F[_]](implicit I: Inject[AuthOp, F]): Auth[F] =
    new Auth
}

class Auth[F[_]](implicit I: Inject[AuthOp, F]) {
  def login(userId: UserId, password: Password): FreeC[F, Option[User]] = 
    App.lift(Login(userId, password))

  def hasPermission(user: User, permission: Permission): FreeC[F, Boolean] =
    App.lift(HasPermission(user, permission))
}

object AuthTest extends (AuthOp ~> Id) {
  override def apply[A](fa: AuthOp[A]) = fa match {
    case Login(userId, password) =>
      if ("1ambda" == userId && "scalaz" == password)
        Some(User("1ambda", Set("scalaz repository", "akka repository")))
      else None
    case HasPermission(user, permission) =>
      user.permissions.contains(permission)
  }
}
