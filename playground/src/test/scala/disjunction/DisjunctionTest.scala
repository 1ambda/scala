package disjunction

import org.scalatest.{Matchers, FunSuite}
import scalaz._
import Scalaz._

// ref: https://vimeo.com/channels/flatmap2015/128466885
// Ref2: http://eed3si9n.com/learning-scalaz/Applicative+Builder.html
// Ref3: https://github.com/scalaz/scalaz/blob/series/7.2.x/core/src/main/scala/scalaz/Either.scala
class EitherTest extends FunSuite with Matchers {

  test("applicative builder test") {
    val result = (3.some |@| 5.some) { _ + _}
    result should be (8.some)
  }

  test("test toDisjunction") {
    val result1 = Some(1) \/> "error" /* toRightDisjunction */
    result1 should be (\/-(1))

    val result2 = None \/> "error"
    result2 should be (-\/("error"))
  }

  test("option might make undiagnosed aborts") {
    import DAO._

    val user1 = for {
      dao <- getDao(0)
      user <- dao.user
    } yield user

    user1 should be (Some(User("1ambda", None)))

    val user2 = for {
      dao <- getDao(1)
      user <- dao.user
    } yield user

    user2 should be (None)
  }

  test("fix undiagnosed abort using disjunction") {
    import DAO._

    val user1 = for {
      dao <- getDao(0) \/> "No user by that ID"
      user <- dao.user \/> "No user object"
    } yield user

    user1 should be (\/-(User("1ambda", None)))

    val user2 = for {
      dao <- getDao(1) \/> "No user by that ID"
      user <- dao.user \/> "No user object"
    } yield user

    user2 should be (-\/("No user object"))
  }

  test("validation rule") {
    1.some *> 2.some should be (2.some)
    1.some <* 2.some should be (1.some)
    1.some <* None   should be (None)
    None   *> 2.some should be (None)

    /* Validation is a subtype of Success, Failure */
    "event1 failure".failure *> "event2 failure".failure should be
      (Failure("event1 failureevent2 failure"))
  }

  test("verify address and user using validation") {
    import DAO._

    val dbObj2 = getDao(2).get
    val result2 = verifyUser(dbObj2) *> verifyAddress(dbObj2.user)
    result2 should be (Success(Address("Seoul")))

    val dbObj3 = getDao(3).get
    val result3 = verifyUser(dbObj3) *> verifyAddress(dbObj3.user)
    result3 should be (Failure("not registered address"))

    val dbObj1 = getDao(1).get
    val result1 = verifyUser(dbObj1) *> verifyAddress(dbObj1.user)
    result1 should be (Failure("DBObject doesn't contain a user objectno such user"))
  }

  test("verify using ValidationNel") {
    import DAO._

    val dbObj1 = getDao(1).get
    val result1 = verifyUserNel(dbObj1) *> verifyAddressNel(dbObj1.user)
    result1 should be (Failure(NonEmptyList(
      "DBObject doesn't contain a user object",
      "no such user")))
  }

  test("applicative builder with ValidationNel") {
    import DAO._

    val dbObj1 = getDao(1).get
    val result1 = (verifyUserNel(dbObj1) |@| verifyAddressNel(dbObj1.user)) {
      case (user, address) => s"User ${user.name} lives in ${address.city}"
    }

    result1 should be (Failure(NonEmptyList(
      "DBObject doesn't contain a user object",
      "no such user")))

    val dbObj2 = getDao(2).get
    val result2 = (verifyUserNel(dbObj2) |@| verifyAddressNel(dbObj2.user)) {
      case (user, address) => s"User ${user.name} lives in ${address.city}"
    }

    result2 should be (Success("User 1ambda lives in Seoul"))
  }

  test("fromTryCatchToThrowable") {
    val result = \/.fromTryCatchThrowable[Int, NumberFormatException] {
      "foo".toInt
    }
  }
}

object DAO {
  case class Address(city: String)
  case class User(name: String, address: Option[Address])
  case class DBObject(id: Long, user: Option[User])

  def getDao(index: Int) = rows(index)

  val row0 = Some(DBObject(0, Some(User("1ambda", None))))
  val row1 = Some(DBObject(1, None))
  val row2 = Some(DBObject(2, Some(User("1ambda", Some(Address("Seoul"))))))
  val row3 = Some(DBObject(3, Some(User("1ambda", Some(Address("Pan-gyo"))))))
  val rows = List(row0, row1, row2, row3)

  def getUser(index: Int) = for {
    dao <- getDao(index) \/> "No user by that ID"
    user <- dao.user \/> "No user object"
  } yield user

  def verifyUser(dbObj: DBObject): Validation[String, User] = {
    dbObj.user match {
      case Some(user) => Success(user)
      case None =>       Failure("DBObject doesn't contain a user object")
    }
  }

  def verifyAddress(user: Option[User]): Validation[String, Address] = {
    user match {
      case Some(User(_, Some(address))) if isValidAddress(address) => address.success
      case Some(User(_, Some(address))) => "not registered address".failure
      case Some(User(_, None))          => "user has no defined address".failure
      case None                         => "no such user".failure
    }
  }

  def isValidAddress(address: Address) = address == Address("Seoul")

  def verifyUserNel(dbObj: DBObject): Validation[NonEmptyList[String], User] = {
    dbObj.user match {
      case Some(user) => Success(user)
      case None =>       Failure(NonEmptyList("DBObject doesn't contain a user object"))
    }
  }

  def verifyAddressNel(user: Option[User]): ValidationNel[String, Address] = {
    user match {
      case Some(User(_, Some(address))) if isValidAddress(address) => address.success
      case Some(User(_, Some(address))) => "not registered address".failureNel
      case Some(User(_, None))          => "user has no defined address".failureNel
      case None                         => "no such user".failureNel
    }
  }
}

