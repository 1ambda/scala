# Scalaz Either `\/`, Validation

[scalaz source - either](https://github.com/scalaz/scalaz/blob/series/7.2.x/core/src/main/scala/scalaz/Either.scala)

```scala
scala> 1.right
res2: scalaz.\/[Nothing,Int] = \/-(1)

scala> 1.right[String]
res3: scalaz.\/[String,Int] = \/-(1)

scala> "error".left[Int]
res4: scalaz.\/[String,Int] = -\/(error)
```

The `Either` in scala standard library is not a monad.

## Disjunction

[Ref - A Practical Scalaz Exploration](https://vimeo.com/channels/flatmap2015/128466885)

- Disjunctions assume we prefer success (Right bias)


```scala
scala> "Success!".right
res0: scalaz.\/[Nothing,String] = \/-(Success!)

scala> -\/("Failure!")
res1: scalaz.-\/[String] = -\/(Failure!)

scala> \/-("Success!")
res2: scalaz.\/-[String] = \/-(Success!)
```

Scala `Option` is a commonly used container, having a `None` and `Some` subtype. 
It is also **Success baised**. But comprehensions over it has issues with **undiagnosed aborts**  

```scala
test("option might make undiagnosed aborts") {
  import DaoUsingOption._

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
```

`\/` to the rescue.

Comprehending over groups of `Option` leads to **silent failure**. Luckily, scalaz includes implicits 
to help convert a `Option` to a `Disjunction`. On a `Left`, we will get potentially useful information instead of `None`  

```scala
test("fix undiagnosed abort using disjunction") {
  import DaoUsingOption._

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
```

Now we have much more useful failure information. But what if we want to do something beyond 
comprehensions?

## Validation

Disjunction is a monad while Validation is not.

Validation is an **applicative functor**, and many can be chained together. 
If any failure in the chain, failure wins. All errors get appended together.

> scalaz has a number of **applicative** operators to combine **Validation** results.

- `*>` takes the right hand value and discards the left
- `<*` takes the left hand value and discards the right
- error wins

```scala
1.some *> 2.some should be (2.some)
1.some <* 2.some should be (1.some)
1.some <* None   should be (None)
None   *> 2.some should be (None)
``` 

And **Validation** appends all errors like

```scala
"event1 failure".failure *> "event2 failure".failure should be
  (Failure("event1 failureevent2 failure"))
```

```scala
object DAO {
  case class Address(city: String)
  case class User(name: String, address: Option[Address])
  case class DBObject(id: Long, user: Option[User])

  def getDao(index: Int) = rows(index)

  val row0 = Some(DBObject(0, Some(User("1ambda", None))))
  val row1 = Some(DBObject(1, None))
  val row2 = Some(DBObject(2, Some(User("1ambda", Some(Address("Seoul"))))))
  val row3 = Some(DBObject(2, Some(User("1ambda", Some(Address("Pan-gyo"))))))
  val rows = List(row0, row1, row2, row3)

  def verifyUser(dbObj: DBObject): Validation[String, User] = {
    dbObj.user match {
      case Some(user) => Success(user)
      case None =>       Failure("DBObject doesn't contain a user object")
    }
  }

  def verifyaddress(user: Option[User]): Validation[String, Address] = {
    user match {
      case Some(User(_, Some(address))) if isValidAddress(address) => address.success
      case Some(User(_, Some(address))) => "not registered address".failure
      case Some(User(_, None))          => "user has no defined address".failure
      case None                         => "no such user".failure
    }
  }

  def isValidAddress(address: Address) = address.city == "Seoul"
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
```

## Non Empty List

`NonEmptyList` is a scalaz `List` that is guaranteed to have at least one element.

- Commonly used with `Validation` to allow accrual of multiple error messages.
- There is a type alias for `Validation[NonEmptyList[L], R]` of `ValidationNEL[L, R]`

```scala
scala> "error".failureNel
res5: scalaz.ValidationNel[String,Nothing] = Failure(NonEmptyList(error))

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

val dbObj1 = getDao(1).get
val result1 = verifyUserNel(dbObj1) *> verifyAddressNel(dbObj1.user)
result1 should be (Failure(NonEmptyList(
  "DBObject doesn't contain a user object",
  "no such user")))
}
```

## |@|

`|@|` combines all of the `Failure` and `Success` conditions. 
To handle `Success`, we provide a `PartialFunction`

```scala
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
```

## Error Handling

- scalaz `\/` offers the higher order function `fromTryCatchThrowable`, 
which catches any specific exceptino, and returns a `Disjunction`

```scala
val result = \/.fromTryCatchThrowable[Int, NumberFormatException] {
  "foo".toInt
}
```
