package free

import scalaz._, Scalaz._
import scalaz.effect._

/**
 * Programs as Values: JDBC Programming with Doobie
 *
 * video - https://www.youtube.com/watch?v=M5MF6M7FHPo
 * slide - http://tpolecat.github.io/assets/sbtb-slides.pdf
 */

trait ResultSet {
  def next: Boolean
  def getString(index: Int): String
  def getInt(index: Int): Int
  def close: Unit
}
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

  val next                 : ResultSetIO[Boolean] = liftFC(Next)
  def getString(index: Int): ResultSetIO[String]  = liftFC(GetString(index))
  def getInt(index: Int)   : ResultSetIO[Int]     = liftFC(GetInt(index))
  def close                : ResultSetIO[Unit]    = liftFC(Close)

  /* for scalaz syntax support */
  implicit val resultSetIOMonadInstance = new Monad[ResultSetIO] {
    override def bind[A, B](fa: ResultSetIO[A])(f: (A) => ResultSetIO[B]): ResultSetIO[B] =
      fa.flatMap(f)

    /* ref - https://gist.github.com/EECOLOR/c312bdf54039a42a3058 */
    override def point[A](a: => A): ResultSetIO[A] =
      Free.point[CoyonedaF[ResultSetOp]#A, A](a)
  }

  def getPerson: ResultSetIO[Person] = for {
    name <- getString(1)
    age  <- getInt(2)
  } yield Person(name, age)

  def getPerson1: ResultSetIO[Person] =
    (getString(1) |@| getInt(2)) { Person(_, _)}

  def getNextPerson: ResultSetIO[Person] =
    next *> getPerson

  def getPeople(n: Int): ResultSetIO[List[Person]] =
    getNextPerson.replicateM(n) // List.fill(n)(getNextPerson).sequence

  def getPersonOpt: ResultSetIO[Option[Person]] =
    next >>= {
      case true  => getPerson.map(_.some)
      case false => none.point[ResultSetIO]
    }

  def getAllPeople: ResultSetIO[Vector[Person]] =
    getPerson.whileM[Vector](next)


  /**
   * To run out program, we should interpret it into some target monad of our choice
   * using natural transformation `ResultSetOp ~> M`
   */

  def interpret(rs: ResultSet) = new (ResultSetOp ~> IO) {
    def apply[A](fa: ResultSetOp[A]): IO[A] = fa match {
      case Next         => IO(rs.next)
      case GetString(i) => IO(rs.getString(i))
      case GetInt(i)    => IO(rs.getInt(i))
      case Close        => IO(rs.close)
    }
  }

  def run[A](a: ResultSetIO[A], rs: ResultSet): IO[A] =
    Free.runFC(a)(interpret(rs))
}




