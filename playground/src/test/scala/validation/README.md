### Validation

[ref: Scalaz Valiation](http://eed3si9n.com/learning-scalaz/Validation.html)

```scala
sealed abstract class Validation[+E, +A] extends Product with Serializable {

  def isSuccess: Boolean = this match {
    case Success(_) => true
    case Failure(_) => false
  }

  /** Return `true` if this validation is failure. */
  def isFailure: Boolean = !isSuccess

  ...
}
```

`Validation` keeps going and reports back **all failures**

```scala
"Validation" should "report all failure" in  {
  val event1: Validation[String, String] = "event1 Failed".failure[String]
  val event2: Validation[String, String] = "event2 Done".success[String]
  val event3: Validation[String, String] = "event3 Failed".failure[String]

  val result: Validation[String, String] =
    (event1 |@| event2 |@| event3) { _ + _ + _ }

  result should be (Failure("event1 Failedevent3 Failed"))
}

```

The problem is, the error messages are mushed together into on string. This is where `NonEmptyList` (`Nel`) comes in

```scala
final class NonEmptyList[+A] private[scalaz](val head: A, val tail: List[A]) {
  def <::[AA >: A](b: AA): NonEmptyList[AA] = nel(b, head :: tail)
  ...
}

"ValidationNel" can "be used to prevent mushed error mesasges" in {
  val nel1: NonEmptyList[Int] = 1.wrapNel
  nel1 should be (NonEmptyList(1))

  val failed1: ValidationNel[String, String] = "failed1".failureNel[String]
  failed1 should be (Failure(NonEmptyList("failed1")))

  val event1: ValidationNel[String, String] = "event1 Failed".failureNel[String]
  val event2: ValidationNel[String, String] = "event2 Done".successNel[String]
  val event3: ValidationNel[String, String] = "event3 Failed".failureNel[String]

  val result: ValidationNel[String, String] =
    (event1 |@| event2 |@| event3) { _ + _ + _ }

  result should be (Failure(NonEmptyList("event1 Failed", "event3 Failed")))
}
```
