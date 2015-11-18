package free

import scalaz._, Scalaz._

/**
 * Programs as Values: JDBC Programming with Doobie
 *
 * video - https://www.youtube.com/watch?v=M5MF6M7FHPo
 * slide - http://tpolecat.github.io/assets/sbtb-slides.pdf
 */

case class Person(name: String, age: Int)

/**
 * def getPerson(rs: ResultSet): Person = {
 *   val name = rs.getString(1)
 *   val age  = rs.getInt(2)
 *
 *   Person(name, age)
 * }
 *
 * - `rs` : managed resource
 * - side-effect (getString, getInt)
 */

sealed trait ResultSetOp[A]
final case object Next extends ResultSetOp[Boolean]
final case class GetString(index: Int) extends ResultSetOp[String]
final case class GetInt(index: Int) extends ResultSetOp[Int]
final case object Close extends ResultSetOp[Unit]

/** if we had Monad[ResultSetOp]
  * val getPerson: ResultSetOp[Person] = for {
  *   name <- GetString(1)
  *   age  <- GetInt(2)
  * } yield Person(name, age)
  *
  *
  * Free[F[_], ?] is a monad for any functor `F`
  * Coyoneda[S[_], ?] is a functor for any `S` at all
  *
  *
  * By substitution, `Free[Coyoneda[S[_], ?], ?]` is a monad for any `S` at all!
  *
  * type FreeC[S[_], A] = Free[({type λ[α] = Coyoneda[S, α]})#λ, A]
  *
  * So, FreeC[ResultSetOp[_], ?] is a monad
  */

object JDBC {
  import Free._, Coyoneda._

  type ResultSetIO[A] = FreeC[ResultSetOp, A]

  val next: ResultSetIO[Boolean] = liftFC(Next)
  def getString(index: Int): ResultSetIO[String] = liftFC(GetString(index))
}




