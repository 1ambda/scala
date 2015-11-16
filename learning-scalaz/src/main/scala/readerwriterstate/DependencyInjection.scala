package readerwriterstate

import scalaz._, Scalaz._

object DependencyInjection {}


/* ref - http://blog.originate.com/blog/2013/10/21/reader-monad-for-dependency-injection/ */

case class User(id: Long,
                name: String,
                age: Int,
                email: String,
                supervisorId: Long)

trait UserRepository {
  def get(id: Long): User
  def find(name: String): User
}

trait UserService {
  def getUser(id: Long): Reader[UserRepository, User] =
    Reader(repo => repo.get(id))

  def findUser(userName: String): Reader[UserRepository, User] =
    Reader(repo => repo.find(userName))

  def getUserInfo(userName: String): Reader[UserRepository, Map[String, String]] = for {
    user <- findUser(userName)
    supervisor <- getUser(user.supervisorId)
  } yield Map(
    "email" -> s"${user.email}",
    "boss"  -> s"${supervisor.name}"
  )
}

object UserRepositoryDummyImpl extends UserRepository {
  override def get(id: Long): User = ???
  override def find(name: String): User = ???
}

class UserApplication(userRepository: UserRepository) extends UserService
object UserApplication extends UserApplication(UserRepositoryDummyImpl)






