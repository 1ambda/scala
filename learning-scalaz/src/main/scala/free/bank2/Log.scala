package free.bank2

import scalaz.{Free, Inject, Id, ~>}, Free._, Id.Id

sealed trait LogOp[A]
final case class Warn(message: String)  extends LogOp[Unit]
final case class Error(message: String) extends LogOp[Unit]
final case class Info(message: String)  extends LogOp[Unit]

class Log[F[_]](implicit I: Inject[LogOp, F]) {
  import Common._

  def warn(message: String)  = lift(Warn(message))
  def info(message: String)  = lift(Info(message))
  def error(message: String) = lift(Error(message))
}

object Log {
  implicit def instant[F[_]](implicit I: Inject[LogOp ,F]) =
    new Log
}

object LogInterpreter extends (LogOp ~> Id) {
  override def apply[A](fa: LogOp[A]): Id.Id[A] = fa match {
    case Warn(message)  =>
      println(s"[WARN] $message")
    case Info(message)  =>
      println(s"[INFO] $message")
    case Error(message) =>
      println(s"[ERROR] $message")
  }
}
